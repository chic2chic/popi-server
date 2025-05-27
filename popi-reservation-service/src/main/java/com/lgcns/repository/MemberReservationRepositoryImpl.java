package com.lgcns.repository;

import static com.lgcns.domain.MemberReservationStatus.RESERVED;
import static com.lgcns.domain.QMemberReservation.memberReservation;

import com.lgcns.domain.MemberReservation;
import com.lgcns.domain.MemberReservationStatus;
import com.lgcns.dto.response.DailyMemberReservationCountResponse;
import com.lgcns.client.managerClient.dto.response.UpcomingReservationResponse;
import com.lgcns.dto.response.DailyReservationCountResponse;
import com.lgcns.dto.response.HourlyReservationCount;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberReservationRepositoryImpl implements MemberReservationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DailyReservationCountResponse> findDailyReservationCount(
            Long popupId, LocalDate popupOpenDate, LocalDate popupCloseDate, String date) {
        YearMonth yearMonth = YearMonth.parse(date);

        LocalDate start = getStartDate(yearMonth, popupOpenDate);
        LocalDate end = getEndDate(yearMonth, popupCloseDate);

        List<Tuple> tuples = getHourlyReservationCounts(popupId, start, end);

        return convertToResponseList(tuples);
    }

    @Override
    public MemberReservation findUpcomingReservation(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate nowDate = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        return queryFactory
                .selectFrom(memberReservation)
                .where(
                        memberReservation.memberId.eq(memberId),
                        memberReservation.status.eq(RESERVED),
                        memberReservation
                                .reservationDate
                                .gt(nowDate)
                                .or(
                                        memberReservation
                                                .reservationDate
                                                .eq(nowDate)
                                                .and(
                                                        memberReservation.reservationTime.goe(
                                                                nowTime))))
                .orderBy(
                        memberReservation.reservationDate.asc(),
                        memberReservation.reservationTime.asc())
                .fetchFirst();
    }

    @Override
    public List<Long> findHotPopupIds() {
        List<Tuple> results =
                queryFactory
                        .select(memberReservation.popupId, memberReservation.popupId.count())
                        .from(memberReservation)
                        .where(memberReservation.popupId.isNotNull())
                        .groupBy(memberReservation.popupId)
                        .orderBy(
                                memberReservation.popupId.count().desc(),
                                memberReservation.popupId.asc())
                        .limit(4)
                        .fetch();

        return extractPopupIds(results);
    }

    @Override
    public List<MemberReservation> findByMemberIdAndStatus(
            Long memberId, MemberReservationStatus status) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate nowDate = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        return queryFactory
                .selectFrom(memberReservation)
                .where(
                        memberReservation.memberId.eq(memberId),
                        memberReservation.status.eq(status),
                        memberReservation
                                .reservationDate
                                .gt(nowDate)
                                .or(
                                        memberReservation
                                                .reservationDate
                                                .eq(nowDate)
                                                .and(
                                                        memberReservation.reservationTime.goe(
                                                                nowTime))))
                .orderBy(
                        memberReservation.reservationDate.asc(),
                        memberReservation.reservationTime.asc())
                .fetch();
    }

    private List<Long> extractPopupIds(List<Tuple> tuples) {
        return tuples.stream()
                .map(tuple -> tuple.get(memberReservation.popupId))
                .collect(Collectors.toList());
    }

    public DailyMemberReservationCountResponse findDailyMemberReservationCount(
            Long popupId, LocalDate today) {
        return queryFactory
                .select(
                        Projections.constructor(
                                DailyMemberReservationCountResponse.class,
                                memberReservation.count()))
                .from(memberReservation)
                .where(
                        memberReservation.popupId.eq(popupId),
                        memberReservation.reservationDate.eq(today))
                .fetchOne();
    }

    public List<UpcomingReservationResponse> findUpcomingReservations() {
        LocalDate today = LocalDate.now();

        return queryFactory
                .select(
                        Projections.constructor(
                                UpcomingReservationResponse.class,
                                memberReservation.memberId,
                                memberReservation.reservationDate))
                .from(memberReservation)
                .where(memberReservation.reservationDate.eq(today.plusDays(1)))
                .fetch();
    }

    private LocalDate getStartDate(YearMonth yearMonth, LocalDate openDate) {
        return YearMonth.from(openDate).equals(yearMonth) ? openDate : yearMonth.atDay(1);
    }

    private LocalDate getEndDate(YearMonth yearMonth, LocalDate closeDate) {
        return YearMonth.from(closeDate).equals(yearMonth) ? closeDate : yearMonth.atEndOfMonth();
    }

    private List<Tuple> getHourlyReservationCounts(Long popupId, LocalDate start, LocalDate end) {
        return queryFactory
                .select(
                        memberReservation.reservationDate,
                        memberReservation.reservationTime,
                        memberReservation.count())
                .from(memberReservation)
                .where(
                        memberReservation.popupId.eq(popupId),
                        memberReservation.reservationDate.between(start, end))
                .groupBy(memberReservation.reservationDate, memberReservation.reservationTime)
                .orderBy(
                        memberReservation.reservationDate.asc(),
                        memberReservation.reservationTime.asc())
                .fetch();
    }

    private List<DailyReservationCountResponse> convertToResponseList(List<Tuple> tuples) {
        Map<LocalDate, List<HourlyReservationCount>> grouped = groupByReservationDate(tuples);
        List<DailyReservationCountResponse> dailyReservationCountList = new ArrayList<>();

        for (Map.Entry<LocalDate, List<HourlyReservationCount>> entry : grouped.entrySet()) {
            dailyReservationCountList.add(
                    new DailyReservationCountResponse(entry.getKey(), entry.getValue()));
        }

        return dailyReservationCountList;
    }

    private Map<LocalDate, List<HourlyReservationCount>> groupByReservationDate(
            List<Tuple> tuples) {
        Map<LocalDate, List<HourlyReservationCount>> groupedByDate = new LinkedHashMap<>();

        for (Tuple tuple : tuples) {
            LocalDate date = tuple.get(memberReservation.reservationDate);
            LocalTime time = tuple.get(memberReservation.reservationTime);
            Long count = tuple.get(memberReservation.count());

            if (!groupedByDate.containsKey(date)) {
                groupedByDate.put(date, new ArrayList<>());
            }

            groupedByDate.get(date).add(new HourlyReservationCount(time, count.intValue()));
        }

        return groupedByDate;
    }
}
