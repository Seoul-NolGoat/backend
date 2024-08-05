package wad.seoul_nolgoat.service.search.sort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wad.seoul_nolgoat.service.search.SearchService;
import wad.seoul_nolgoat.service.search.dto.*;
import wad.seoul_nolgoat.service.tMap.TMapService;
import wad.seoul_nolgoat.service.tMap.dto.WalkRouteInfoDto;
import wad.seoul_nolgoat.web.search.dto.CoordinateDto;

import java.util.*;

@RequiredArgsConstructor
@Service
public class SortService {

    private static final int TOP_FIRST = 0;
    private static final int TOP_TWENTIETH = 20;

    private final TMapService tMapService;
    private final DistanceCalculator distanceCalculator;

    public List<GradeSortCombinationDto> sortStoresByGrade(SortConditionDto<StoreForGradeSortDto> sortConditionDto) {
        int totalRounds = sortConditionDto.getTotalRounds();
        List<GradeSortCombinationDto> gradeCombinations = generateGradeCombinations(sortConditionDto, totalRounds);

        return sortCombinationsByGrade(gradeCombinations, totalRounds);
    }

    public List<DistanceSortCombinationDto> sortStoresByDistance(SortConditionDto<StoreForDistanceSortDto> sortConditionDto) {
        int totalRounds = sortConditionDto.getTotalRounds();
        List<DistanceSortCombinationDto> distanceCombinations = generateAndSortDistanceCombinations(
                sortConditionDto,
                totalRounds
        ).subList(TOP_FIRST, TOP_TWENTIETH);

        return fetchDistancesFromTMapApi(
                sortConditionDto.getStartCoordinate(),
                distanceCombinations,
                totalRounds
        );
    }

    // 테스트를 위해 접근제어자를 public으로 변경
    public List<DistanceSortCombinationDto> generateAndSortDistanceCombinations(
            SortConditionDto<StoreForDistanceSortDto> sortConditionDto,
            int totalRounds) {
        if (totalRounds == SearchService.THREE_ROUND) {
            return createDistanceCombinationsForThreeRounds(
                    sortConditionDto.getFirstFilteredStores(),
                    sortConditionDto.getSecondFilteredStores(),
                    sortConditionDto.getThirdFilteredStores(),
                    sortConditionDto.getStartCoordinate()
            ).stream()
                    .sorted(Comparator.comparingDouble(DistanceSortCombinationDto::getTotalDistance))
                    .toList();
        }
        if (totalRounds == SearchService.TWO_ROUND) {
            return createDistanceCombinationsForTwoRounds(
                    sortConditionDto.getFirstFilteredStores(),
                    sortConditionDto.getSecondFilteredStores(),
                    sortConditionDto.getStartCoordinate()
            ).stream()
                    .sorted(Comparator.comparingDouble(DistanceSortCombinationDto::getTotalDistance))
                    .toList();
        }
        if (totalRounds == SearchService.ONE_ROUND) {
            return createDistanceCombinationsForOneRound(
                    sortConditionDto.getFirstFilteredStores(),
                    sortConditionDto.getStartCoordinate()
            ).stream()
                    .sorted(Comparator.comparingDouble(DistanceSortCombinationDto::getTotalDistance))
                    .toList();
        }
        throw new RuntimeException();
    }

    private List<GradeSortCombinationDto> generateGradeCombinations(
            SortConditionDto<StoreForGradeSortDto> sortConditionDto,
            int totalRounds) {
        if (totalRounds == SearchService.THREE_ROUND) {
            return createGradeCombinationsForThreeRounds(
                    sortConditionDto.getFirstFilteredStores(),
                    sortConditionDto.getSecondFilteredStores(),
                    sortConditionDto.getThirdFilteredStores()
            );
        }
        if (totalRounds == SearchService.TWO_ROUND) {
            return createGradeCombinationsForTwoRounds(
                    sortConditionDto.getFirstFilteredStores(),
                    sortConditionDto.getSecondFilteredStores()
            );
        }
        if (totalRounds == SearchService.ONE_ROUND) {
            return createGradeCombinationsForOneRound(sortConditionDto.getFirstFilteredStores());
        }
        throw new RuntimeException();
    }

    private List<GradeSortCombinationDto> createGradeCombinationsForThreeRounds(
            List<StoreForGradeSortDto> firstStores,
            List<StoreForGradeSortDto> secondStores,
            List<StoreForGradeSortDto> thirdStores) {
        List<GradeSortCombinationDto> gradeCombinations = new ArrayList<>();
        for (StoreForGradeSortDto firstStore : firstStores) {
            for (StoreForGradeSortDto secondStore : secondStores) {
                if (hasDuplicateStores(firstStore, secondStore)) {
                    continue;
                }
                for (StoreForGradeSortDto thirdStore : thirdStores) {
                    if (hasDuplicateStores(
                            firstStore,
                            secondStore,
                            thirdStore
                    )) {
                        continue;
                    }
                    gradeCombinations.add(
                            new GradeSortCombinationDto(
                                    firstStore,
                                    secondStore,
                                    thirdStore
                            )
                    );
                }
            }
        }

        return gradeCombinations;
    }

    private List<GradeSortCombinationDto> createGradeCombinationsForTwoRounds(
            List<StoreForGradeSortDto> firstStores,
            List<StoreForGradeSortDto> secondStores) {
        List<GradeSortCombinationDto> gradeCombinations = new ArrayList<>();
        for (StoreForGradeSortDto firstStore : firstStores) {
            for (StoreForGradeSortDto secondStore : secondStores) {
                if (hasDuplicateStores(firstStore, secondStore)) {
                    continue;
                }
                gradeCombinations.add(new GradeSortCombinationDto(firstStore, secondStore));
            }
        }

        return gradeCombinations;
    }

    private List<GradeSortCombinationDto> createGradeCombinationsForOneRound(List<StoreForGradeSortDto> firstStores) {
        List<GradeSortCombinationDto> gradeCombinations = new ArrayList<>();
        for (StoreForGradeSortDto firstStore : firstStores) {
            gradeCombinations.add(new GradeSortCombinationDto(firstStore));
        }

        return gradeCombinations;
    }

    private List<GradeSortCombinationDto> sortCombinationsByGrade(
            List<GradeSortCombinationDto> combinations,
            int totalRounds) {
        if (totalRounds == SearchService.THREE_ROUND) {
            combinations.sort((combination1, combination2) -> {
                double firstRate = combination1.getFirstStore().getAverageGrade()
                        + combination1.getSecondStore().getAverageGrade()
                        + combination1.getThirdStore().getAverageGrade();
                double secondRate = combination2.getFirstStore().getAverageGrade()
                        + combination2.getSecondStore().getAverageGrade()
                        + combination2.getThirdStore().getAverageGrade();

                return Double.compare(secondRate, firstRate);
            });
        }
        if (totalRounds == SearchService.TWO_ROUND) {
            combinations.sort((combination1, combination2) -> {
                double firstRate = combination1.getFirstStore().getAverageGrade()
                        + combination1.getSecondStore().getAverageGrade();
                double secondRate = combination2.getFirstStore().getAverageGrade()
                        + combination2.getSecondStore().getAverageGrade();

                return Double.compare(secondRate, firstRate);
            });
        }
        if (totalRounds == SearchService.ONE_ROUND) {
            combinations.sort((combination1, combination2) -> {
                double firstRate = combination1.getFirstStore().getAverageGrade();
                double secondRate = combination2.getFirstStore().getAverageGrade();

                return Double.compare(secondRate, firstRate);
            });
        }

        return combinations;
    }

    private List<DistanceSortCombinationDto> createDistanceCombinationsForThreeRounds(
            List<StoreForDistanceSortDto> firstStores,
            List<StoreForDistanceSortDto> secondStores,
            List<StoreForDistanceSortDto> thirdStores,
            CoordinateDto coordinateDto) {
        List<DistanceSortCombinationDto> combinations = new ArrayList<>();
        for (StoreForDistanceSortDto firstStore : firstStores) {
            for (StoreForDistanceSortDto secondStore : secondStores) {
                if (hasDuplicateStores(firstStore, secondStore)) {
                    continue;
                }
                for (StoreForDistanceSortDto thirdStore : thirdStores) {
                    if (hasDuplicateStores(
                            firstStore,
                            secondStore,
                            thirdStore
                    )) {
                        continue;
                    }
                    DistanceSortCombinationDto distanceSortCombinationDto = new DistanceSortCombinationDto(
                            firstStore,
                            secondStore,
                            thirdStore
                    );
                    double totalDistance = distanceCalculator.calculateTotalDistance(
                            distanceSortCombinationDto.getFirstStore(),
                            distanceSortCombinationDto.getSecondStore(),
                            distanceSortCombinationDto.getThirdStore(),
                            coordinateDto
                    );
                    distanceSortCombinationDto.setTotalDistance(totalDistance);
                    combinations.add(distanceSortCombinationDto);
                }
            }
        }

        return combinations;
    }

    private List<DistanceSortCombinationDto> createDistanceCombinationsForTwoRounds(
            List<StoreForDistanceSortDto> firstStores,
            List<StoreForDistanceSortDto> secondStores,
            CoordinateDto coordinateDto) {
        DistanceCalculator distanceCalculator = new DistanceCalculator();
        List<DistanceSortCombinationDto> combinations = new ArrayList<>();
        for (StoreForDistanceSortDto firstStore : firstStores) {
            for (StoreForDistanceSortDto secondStore : secondStores) {
                if (hasDuplicateStores(firstStore, secondStore)) {
                    continue;
                }
                DistanceSortCombinationDto distanceSortCombinationDto = new DistanceSortCombinationDto(
                        firstStore,
                        secondStore
                );
                double totalDistance = distanceCalculator.calculateTotalDistance(
                        distanceSortCombinationDto.getFirstStore(),
                        distanceSortCombinationDto.getSecondStore(),
                        coordinateDto);
                distanceSortCombinationDto.setTotalDistance(totalDistance);
                combinations.add(distanceSortCombinationDto);
            }
        }

        return combinations;
    }

    private List<DistanceSortCombinationDto> createDistanceCombinationsForOneRound(
            List<StoreForDistanceSortDto> firstStores,
            CoordinateDto coordinateDto) {
        DistanceCalculator distanceCalculator = new DistanceCalculator();
        List<DistanceSortCombinationDto> combinations = new ArrayList<>();
        for (StoreForDistanceSortDto firstStore : firstStores) {
            DistanceSortCombinationDto distanceSortCombinationDto = new DistanceSortCombinationDto(firstStore);
            double totalDistance = distanceCalculator.calculateTotalDistance(
                    distanceSortCombinationDto.getFirstStore(), coordinateDto);
            distanceSortCombinationDto.setTotalDistance(totalDistance);
            combinations.add(distanceSortCombinationDto);
        }

        return combinations;
    }

    private List<DistanceSortCombinationDto> fetchDistancesFromTMapApi(
            CoordinateDto startCoordinate,
            List<DistanceSortCombinationDto> distanceSortCombinationDtos,
            int totalRounds) {
        List<DistanceSortCombinationDto> combinations = distanceSortCombinationDtos.parallelStream()
                .map(combination -> {
                    if (totalRounds == SearchService.THREE_ROUND) {
                        CoordinateDto pass1 = combination.getFirstStore().getCoordinate();
                        CoordinateDto pass2 = combination.getSecondStore().getCoordinate();
                        CoordinateDto endCoordinate = combination.getThirdStore().getCoordinate();

                        return new DistanceSortCombinationDto(
                                combination.getFirstStore(),
                                combination.getSecondStore(),
                                combination.getThirdStore(),
                                fetchWalkRouteInfo(
                                        startCoordinate,
                                        pass1,
                                        pass2,
                                        endCoordinate
                                )
                        );
                    }
                    if (totalRounds == SearchService.TWO_ROUND) {
                        CoordinateDto pass = combination.getFirstStore().getCoordinate();
                        CoordinateDto endCoordinate = combination.getSecondStore().getCoordinate();

                        return new DistanceSortCombinationDto(
                                combination.getFirstStore(),
                                combination.getSecondStore(),
                                fetchWalkRouteInfo(
                                        startCoordinate,
                                        pass,
                                        endCoordinate
                                )
                        );
                    }
                    if (totalRounds == SearchService.ONE_ROUND) {
                        CoordinateDto endCoordinate = combination.getFirstStore().getCoordinate();

                        return new DistanceSortCombinationDto(
                                combination.getFirstStore(),
                                fetchWalkRouteInfo(startCoordinate, endCoordinate)
                        );
                    }
                    throw new RuntimeException("Invalid round number");
                }).toList();

        return combinations.stream()
                .sorted(Comparator.comparingInt(combination -> combination.getWalkRouteInfoDto().getTotalDistance()))
                .toList();
    }

    private boolean hasDuplicateStores(
            StoreForDistanceSortDto firstStore,
            StoreForDistanceSortDto secondStore,
            StoreForDistanceSortDto thirdStore) {
        Set<Long> storeIds = new HashSet<>();
        storeIds.add(firstStore.getId());
        storeIds.add(secondStore.getId());
        storeIds.add(thirdStore.getId());

        return storeIds.size() != SearchService.THREE_ROUND;
    }

    private boolean hasDuplicateStores(StoreForDistanceSortDto firstStore, StoreForDistanceSortDto secondStore) {
        Set<Long> storeIds = new HashSet<>();
        storeIds.add(firstStore.getId());
        storeIds.add(secondStore.getId());

        return storeIds.size() != SearchService.TWO_ROUND;
    }

    private boolean hasDuplicateStores(
            StoreForGradeSortDto firstStore,
            StoreForGradeSortDto secondStore,
            StoreForGradeSortDto thirdStore) {
        Set<Long> storeIds = new HashSet<>();
        storeIds.add(firstStore.getId());
        storeIds.add(secondStore.getId());
        storeIds.add(thirdStore.getId());

        return storeIds.size() != SearchService.THREE_ROUND;
    }

    private WalkRouteInfoDto fetchWalkRouteInfo(
            CoordinateDto startCoordinate,
            CoordinateDto pass1,
            CoordinateDto pass2,
            CoordinateDto endCoordinate) {
        if (isSameLocation(startCoordinate, pass1) && isSameLocation(pass1, pass2) && isSameLocation(pass2, endCoordinate)) {
            return new WalkRouteInfoDto(0, 0);
        }
        if (isSameLocation(startCoordinate, pass1) && isSameLocation(pass1, pass2)) {
            return tMapService.fetchWalkRouteInfo(pass2, endCoordinate);
        }
        if (isSameLocation(pass1, pass2) && isSameLocation(pass2, endCoordinate)) {
            return tMapService.fetchWalkRouteInfo(startCoordinate, pass1);
        }
        if (isSameLocation(startCoordinate, pass1) && isSameLocation(pass2, endCoordinate)) {
            return tMapService.fetchWalkRouteInfo(startCoordinate, pass1, endCoordinate);
        }
        if (isSameLocation(startCoordinate, pass1)) {
            return tMapService.fetchWalkRouteInfo(pass1, pass2, endCoordinate);
        }
        if (isSameLocation(pass1, pass2)) {
            WalkRouteInfoDto firstWalkRouteInfo = tMapService.fetchWalkRouteInfo(startCoordinate, pass1);
            WalkRouteInfoDto secondWalkRouteInfo = tMapService.fetchWalkRouteInfo(pass2, endCoordinate);

            return new WalkRouteInfoDto(
                    firstWalkRouteInfo.getTotalDistance() + secondWalkRouteInfo.getTotalDistance(),
                    firstWalkRouteInfo.getTotalTime() + secondWalkRouteInfo.getTotalTime()
            );
        }

        return tMapService.fetchWalkRouteInfo(startCoordinate, pass1, pass2, endCoordinate);
    }

    private WalkRouteInfoDto fetchWalkRouteInfo(
            CoordinateDto startCoordinate,
            CoordinateDto pass,
            CoordinateDto endCoordinate) {
        if (isSameLocation(startCoordinate, pass) && isSameLocation(pass, endCoordinate)) {
            return new WalkRouteInfoDto(0, 0);
        }
        if (isSameLocation(startCoordinate, pass)) {
            return tMapService.fetchWalkRouteInfo(pass, endCoordinate);
        }
        if (isSameLocation(pass, endCoordinate)) {
            return tMapService.fetchWalkRouteInfo(startCoordinate, pass);
        }

        return tMapService.fetchWalkRouteInfo(startCoordinate, pass, endCoordinate);
    }

    private WalkRouteInfoDto fetchWalkRouteInfo(
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate) {
        if (isSameLocation(startCoordinate, endCoordinate)) {
            return new WalkRouteInfoDto(0, 0);
        }

        return tMapService.fetchWalkRouteInfo(startCoordinate, endCoordinate);
    }

    private boolean hasDuplicateStores(StoreForGradeSortDto firstStore, StoreForGradeSortDto secondStore) {
        Set<Long> storeIds = new HashSet<>();
        storeIds.add(firstStore.getId());
        storeIds.add(secondStore.getId());

        return storeIds.size() != SearchService.TWO_ROUND;
    }

    private boolean isSameLocation(CoordinateDto coordinate1, CoordinateDto coordinate2) {
        return (coordinate1.getLongitude() == coordinate2.getLongitude())
                && (coordinate1.getLatitude() == coordinate2.getLatitude());
    }
}
