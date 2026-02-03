package com.project.dykj.domain.notification.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.project.dykj.domain.notification.repository.HoldingMapper;
import com.project.dykj.domain.notification.repository.NotificationRepository;
import com.project.dykj.domain.notification.service.NotificationService;
import com.project.dykj.domain.notification.dto.NotificationVO;
import com.project.dykj.kis.service.StockService;
import com.project.dykj.domain.stock.vo.Holding;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.project.dykj.domain.member.mapper.MemberMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStockScheduler {

    private final HoldingMapper holdingMapper;
    private final StockService stockService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final MemberMapper memberMapper;

    @Scheduled(cron = "0 * * * * *") // 테스트를 위해 1분마다 실행
    public void checkPriceFluctuation() {
        log.info("=== [알림 시스템] 전일 대비 등락 감시 시작 ===");

        List<Holding> holdings = holdingMapper.selectAllHoldings();
        if (holdings == null || holdings.isEmpty())
            return;

        List<String> stockIds = holdings.stream()
                .map(Holding::getStockId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Object> currentPrices = stockService.getMultiplePrices(stockIds);

        for (Holding h : holdings) {
            try {
                Object priceObj = currentPrices.get(h.getStockId());
                if (!(priceObj instanceof Map<?, ?> priceMap))
                    continue;

                // [수정] 전일 대비 등락률(prdy_ctrt)을 API에서 직접 가져옵니다.
                Object ctrtObj = priceMap.get("prdy_ctrt");
                if (ctrtObj == null)
                    continue;

                double changeRate = Double.parseDouble(String.valueOf(ctrtObj));

                // 1. 등락폭이 5% 이상인지 확인
                if (Math.abs(changeRate) >= 5.0) {

                    // 2. 중요: 오늘 이미 이 종목으로 알림을 보냈는지 DB 확인
                    int sentCount = notificationRepository.countTodayStockNotification(h.getUserId(), h.getStockId());

                    if (sentCount == 0) {
                        NotificationVO noti = NotificationVO.builder()
                                .userId(h.getUserId())
                                .notiType("PRICE_ALERT")
                                .notiChannel("WEB")
                                .message(String.format("[%s] 종목이 전일 종가 대비 %.2f%% %s 중입니다!",
                                        h.getStockId(), changeRate, changeRate > 0 ? "상승" : "하락"))
                                .notiUrl("/stock/" + h.getStockId())
                                .isRead("N")
                                .build();

                        notificationService.createNotification(noti);
                        log.info(">>> [알림 신규발송] {} 유저 - {} 종목 (등락률: {}%)", h.getUserId(), h.getStockId(), changeRate);
                    } else {
                        log.info("--- [발송 건너뜀] {} 유저는 오늘 이미 {} 알림을 받았습니다.", h.getUserId(), h.getStockId());
                    }
                }
            } catch (Exception e) {
                log.error("주가 체크 오류 ({}): {}", h.getStockId(), e.getMessage());
            }
        }
    }

    

    // 1. 매일 아침 09:00 장 시작 알림
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyMarketOpen() {
        log.info("=== [알림 시스템] 장 시작 알림 발송 ===");
        sendGlobalNotification("MARKET_OPEN", "주식 장이 시작되었습니다! 성투하세요! 📈", "/stock");
    }

    // 2. 매일 오후 15:30 장 마감 알림
    @Scheduled(cron = "0 30 15 * * *")
    public void notifyMarketClose() {
        log.info("=== [알림 시스템] 장 마감 알림 발송 ===");
        sendGlobalNotification("MARKET_CLOSE", "주식 장이 마감되었습니다. 오늘 하루도 고생하셨습니다! 👏", "/stock");
    }

    // [공통 메서드] 모든 유저에게 알림 보내기
    private void sendGlobalNotification(String type, String message, String url) {
        try {
            // 1. 모든 유저 ID 가져오기
            List<String> allUsers = memberMapper.selectAllMemberIds();

            // 2. 각 유저에게 알림 생성
            for (String userId : allUsers) {
                NotificationVO noti = NotificationVO.builder()
                        .userId(userId)
                        .notiType(type)
                        .notiChannel("WEB") // 웹 알림
                        .message(message)
                        .notiUrl(url)
                        .isRead("N")
                        .build();

                notificationService.createNotification(noti);
            }
            log.info(">>> [전체 알림 발송 완료] 총 {}명", allUsers.size());

        } catch (Exception e) {
            log.error("전체 알림 발송 실패: {}", e.getMessage());
        }
    }
}