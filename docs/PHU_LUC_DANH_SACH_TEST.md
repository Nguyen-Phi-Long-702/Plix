# Phụ lục — Danh sách toàn bộ test case

Tạo tự động từ mã nguồn (Android: các hàm `@Test`; Backend: `pytest --collect-only`). Kết quả đạt/không đạt của từng nhóm nằm trong `docs/BAO_CAO_KIEM_THU.md`.

## 1. Unit test Android (`android/app/src/test`) — 409 test

### `android/app/src/test/java/com/longvuong/plix/core/auth/AuthAuthenticatorTest.java` (2)

- `authenticate_expiredToken_refreshesOnceAndRetriesWithNewToken`
- `authenticate_manyConcurrent401_refreshesExactlyOnce`

### `android/app/src/test/java/com/longvuong/plix/core/auth/AuthManagerTest.java` (5)

- `getCurrentUserEmail_tokenHasEmailClaim_returnsEmail`
- `getCurrentUserEmail_tokenWithoutEmailClaim_returnsNull`
- `getCurrentUserEmail_noToken_returnsNull`
- `getCurrentUserEmail_malformedToken_returnsNull`
- `getCurrentUserId_tokenHasSubClaim_returnsSub`

### `android/app/src/test/java/com/longvuong/plix/core/error/ErrorMapperTest.java` (18)

- `mapThrowable_ioException_returnsNetworkError`
- `mapThrowable_unknownException_returnsUnknownError`
- `mapHttpCode_401_returnsAuthError`
- `mapHttpCode_500_returnsServerErrorWithCorrectMessage`
- `mapHttpCode_503_returnsServerError`
- `mapHttpCode_400_withServerBody_returnsServerMessage`
- `mapHttpCode_400_withoutBody_returnsFallbackMessage`
- `mapAiBusy_returnsAiBusyErrorWithFriendlyMessage`
- `mapHttpCode_503_withAiBusyBody_returnsAiBusyError`
- `mapHttpCode_503_withOtherBody_stillReturnsServerError`
- `mapHttpCode_503_withInvalidJsonBody_stillReturnsServerError`
- `mapAuthHttpCode_invalidCredentials_returnsFriendlyAuthError`
- `mapAuthHttpCode_userAlreadyExists_returnsEmailRegisteredMessage`
- `mapAuthHttpCode_weakPassword_returnsWeakPasswordMessage`
- `mapAuthHttpCode_invalidEmail_returnsInvalidEmailMessage`
- `mapAuthHttpCode_429_returnsRetryLaterMessage`
- `mapAuthHttpCode_500_returnsServerBusyMessage`
- `mapAuthHttpCode_unknownError_neverShowsHttpCode`

### `android/app/src/test/java/com/longvuong/plix/data/sync/DeletedCategoryNoticeTest.java` (7)

- `checkAfterPull_categoryDeletedElsewhereAndTransactionStillUsesIt_showsNotice`
- `checkAfterPull_categoryDeletedElsewhereButNoTransactionUsesIt_noNotice`
- `checkAfterPull_onlySoftDeletedTransactionUsesIt_noNotice`
- `checkAfterPull_categoryReceivedAlreadyDeletedAndTransactionUsesIt_showsNotice`
- `checkAfterPull_categoryWasAlreadyDeletedBeforeAndOverwritten_noNotice`
- `checkAfterPull_categoryChangedButNotDeleted_noNotice`
- `markHandled_afterNotice_resetsToFalse`

### `android/app/src/test/java/com/longvuong/plix/data/sync/RecurringTransactionWorkerTest.java` (17)

- `generateMissingInstances_templateWithoutInstanceYet_insertsExactlyOneInstance`
- `generateMissingInstances_calledTwiceSamePeriod_doesNotCreateDuplicate`
- `generateMissingInstances_generatedInstance_isNotItselfRecurringAndLinksToTemplate`
- `generateMissingInstances_invalidRecurrenceRule_doesNotInsertAnyInstance`
- `parseDayOfMonth_validRule_returnsDay`
- `parseDayOfMonth_nullOrWrongPrefix_returnsInvalid`
- `generateMissingInstances_threeConsecutiveMonthsMissed_catchesUpExactlyThreeInstances`
- `generateMissingInstances_workerRunsEveryMonthInSequence_generatesOneInstancePerMonthNoDuplicates`
- `generateMissingInstances_day31RuleInto30DayMonth_clampsToDay30`
- `generateMissingInstances_day31RuleIntoNonLeapFebruary_clampsToDay28`
- `generateMissingInstances_day31RuleIntoLeapFebruary_clampsToDay29`
- `editingRecurrenceRuleAfterInstanceGenerated_doesNotChangePastInstance`
- `softDeletingTemplate_stopsGeneratingNewInstances_butKeepsAlreadyGeneratedOnes`
- `deletingGeneratedInstance_doesNotRegenerateOnNextWorkerRun`
- `isEligibleToRun_noSession_returnsFalse`
- `isEligibleToRun_hasAccessTokenInMemory_returnsTrue`
- `isEligibleToRun_afterRealLogout_returnsFalse`

### `android/app/src/test/java/com/longvuong/plix/data/sync/SyncLockTest.java` (2)

- `awaitIdle_whenNobodyHoldsLock_returnsImmediately`
- `awaitIdle_whileWorkerHoldsLock_waitsUntilWorkerFinishes`

### `android/app/src/test/java/com/longvuong/plix/data/sync/SyncPullerTest.java` (23)

- `pullAll_emptyFirstResponse_appliesNothingAndKeepsCursor`
- `pullAll_singleBatchWithoutMore_appliesOnceAndSavesMaxUpdatedAt`
- `pullAll_moreThan500Records_loopsWithNewCursorUntilAllPulled`
- `pullAll_501RecordsWithSameUpdatedAt_stopsWhenSameBatchReturnedTwice`
- `pullAll_networkErrorOnSecondCall_keepsCursorOfFirstBatch`
- `pullAll_applyFailsOnSecondBatch_cursorStaysAtFirstBatch`
- `pullAll_serverErrorOnFirstCall_neverAppliesAndKeepsExistingCursor`
- `pullAll_serverErrorOnSecondCall_keepsCursorOfFirstBatch`
- `pullAll_freshInstallReceivesTombstone_storesItAsDeletedAndKeepsItHidden`
- `pullAll_exactly500Records_stopsAfterOneCallWhenServerSaysNoMore`
- `pullAll_shouldStopAlreadyTrue_doesNotCallApi`
- `upsertBatch_unknownRecord_insertsAsSyncedForCurrentUser`
- `upsertBatch_incomingNewer_overwritesExisting`
- `upsertBatch_sameUpdatedAt_keepsExisting`
- `upsertBatch_incomingOlder_keepsExisting`
- `upsertBatch_incomingTombstoneNewer_marksDeletedLocally`
- `upsertBatch_localPendingNewerThanIncoming_staysPendingAndUnchanged`
- `upsertBatch_unknownRecord_reportsInsertedChangeWithoutBefore`
- `upsertBatch_incomingNewer_reportsChangeWithOldAndNewVersion`
- `upsertBatch_incomingNotNewer_reportsNoChange`
- `upsertBatch_localPendingSameUpdatedAtAsIncoming_keepsLocalAndStaysPending`
- `upsertBatch_differentUpdatedAt_newerWinsInEitherArrivalOrder`
- `upsertBatch_sameUpdatedAt_firstStoredVersionWinsAndLaterOneNeverOverwrites`

### `android/app/src/test/java/com/longvuong/plix/data/sync/SyncPusherTest.java` (5)

- `push_networkLostOnSecondBatch_firstBatchSyncedRestStayPending`
- `push_afterNetworkReturns_remainingPendingRecordsGetSynced`
- `push_120PendingRecords_sendsBatchesOf50And50And20_eachIdExactlyOnce`
- `push_exactly100PendingRecords_sendsTwoFullBatchesAndNoEmptyBatch`
- `push_120PendingNetworkLostOnSecondBatch_retryOnlySendsTheRemaining70`

### `android/app/src/test/java/com/longvuong/plix/data/sync/SyncTablesTest.java` (1)

- `inSyncOrder_isCategoriesTransactionsBudgetsGoalsCorrections`

### `android/app/src/test/java/com/longvuong/plix/data/sync/SyncTombstoneTest.java` (5)

- `transaction_deletedEntity_keepsIsDeletedWhenPushedAndPulled`
- `category_deletedEntity_keepsIsDeletedWhenPushedAndPulled`
- `budget_deletedEntity_keepsIsDeletedWhenPushedAndPulled`
- `goal_deletedEntity_keepsIsDeletedWhenPushedAndPulled`
- `correction_deletedEntity_keepsIsDeletedWhenPushedAndPulled`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/analytics/AggregateAnalyticsUseCaseTest.java` (9)

- `execute_emptyList_returnsZeroedSummaryWithoutCrash`
- `execute_nullList_returnsZeroedSummaryWithoutCrash`
- `execute_groupsExpenseByCategory_sortedDescendingByAmount`
- `execute_expenseWithoutCategory_groupedUnderNullKey`
- `execute_excludesDeletedTransactions`
- `execute_monthlyTrend_sortedChronologicallyWithIncomeAndExpenseSeparated`
- `execute_totalIncomeAndExpense_summedAcrossWholeList`
- `execute_savingsRate_calculatedCorrectly`
- `execute_savingsRate_zeroIncome_doesNotDivideByZero`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/analytics/CalculateMonthToDateNetUseCaseTest.java` (7)

- `execute_emptyList_returnsZeroSeriesUpToToday`
- `execute_nullList_returnsZeroSeriesUpToToday`
- `execute_incomeAndExpense_accumulatesNetPerDay`
- `execute_deletedTransaction_isIgnored`
- `execute_transactionsOfOtherMonths_areIgnored`
- `execute_futureDaysOfSameMonth_areIgnored`
- `execute_firstDayOfMonth_returnsSingleElement`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/auth/LogoutUseCaseTest.java` (7)

- `start_noPending_logsOutImmediatelyWithoutFinalSync`
- `start_pendingOnlineAndFinalSyncSucceeds_logsOutAfterCancelAndPush`
- `start_pendingAndOffline_skipsFinalSyncAndAsksConfirmation`
- `start_finalSyncTimesOut_asksConfirmationWithoutClearing`
- `start_finalSyncPartiallySucceeds_reportsRemainingCount`
- `confirmLogoutAnyway_clearsLocalSession`
- `cancelLogout_resumesSyncAndDoesNotClear`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/budget/AddBudgetUseCaseTest.java` (8)

- `execute_firstOverallBudgetOfMonth_insertsSuccessfully`
- `execute_secondOverallBudgetSameMonth_rejectsWithCorrectMessage`
- `execute_overallBudgetDifferentMonth_insertsSuccessfully`
- `execute_categoryBudget_insertsSuccessfullyEvenIfOverallBudgetExists`
- `execute_invalidLimitAmount_rejectsAndDoesNotInsert`
- `execute_invalidPeriod_rejectsAndDoesNotInsert`
- `execute_secondCategoryBudgetSamePeriodAndCategory_rejectsWithCorrectMessage`
- `execute_categoryBudgetDifferentCategorySamePeriod_insertsSuccessfully`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/budget/BudgetThresholdCheckerTest.java` (7)

- `justCrossedThreshold_beforeZeroAfterZero_returnsFalse`
- `justCrossedThreshold_before79AfterStill79_returnsFalse`
- `justCrossedThreshold_before79AfterExactly80_returnsTrue`
- `justCrossedThreshold_alreadyAtThresholdBefore_doesNotNotifyAgain`
- `justCrossedThreshold_before79AfterExactly100_returnsTrue`
- `justCrossedThreshold_alreadyOver100Before_doesNotNotifyAgain`
- `justCrossedThreshold_limitAmountZero_returnsFalseWithoutDivideByZero`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/budget/CalculateBudgetProgressUseCaseTest.java` (7)

- `execute_categoryBudget_onlySumsMatchingCategoryExpense`
- `execute_overallBudget_includesUncategorizedTransactions`
- `execute_excludesDeletedTransactions`
- `execute_excludesIncomeTransactions`
- `execute_excludesTransactionsOutsidePeriod`
- `execute_limitAmountZero_doesNotDivideByZero`
- `periodStartAndEndAtMillis_coverWholeMonth`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/budget/CheckBudgetThresholdUseCaseTest.java` (13)

- `checkAfterAdd_pushesOverallBudgetOverThreshold_notifies`
- `checkAfterAdd_stillBelowThreshold_doesNotNotify`
- `checkAfterAdd_categoryBudgetCrossed_notifiesOnlyThatBudget`
- `checkAfterAdd_noBudgetForScope_doesNotNotifyOrCrash`
- `checkAfterUpdate_editAmountPushesOverThreshold_notifies`
- `checkAfterUpdate_editStillBelowThreshold_doesNotNotify`
- `checkAfterUpdate_categoryChangedPushesNewCategoryBudgetOverThreshold_notifies`
- `checkAfterPull_pulledTransactionPushesBudgetOverThreshold_notifies`
- `checkAfterPull_severalPulledTransactionsSameBudget_notifiesOnlyOnce`
- `checkAfterPull_pulledEditOverwritesLocalAndCrossesThreshold_notifies`
- `checkAfterPull_alreadyAboveThresholdBeforePull_doesNotNotifyAgain`
- `checkAfterPull_pulledTransactionStillBelowThreshold_doesNotNotify`
- `checkAfterPull_nothingPulled_doesNotNotifyOrCrash`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/budget/DeleteBudgetUseCaseTest.java` (2)

- `execute_ownedByCurrentUser_marksSoftDeleteFieldsAndUpdates`
- `execute_ownedByAnotherUser_rejectsBeforeUpdate`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/budget/UpdateBudgetUseCaseTest.java` (3)

- `execute_validLimitAmount_updatesSuccessfully`
- `execute_validLimitAmount_setsUpdatedAtAndSyncStatusPending`
- `execute_invalidLimitAmount_rejectsAndDoesNotUpdate`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/category/CategoryUseCaseTest.java` (14)

- `addCategory_newNameNotDuplicateWithSystem_insertsAndReturnsSuccess`
- `addCategory_duplicateNameAndTypeWithSystemCategory_rejectsBeforeInsert`
- `addCategory_sameNameDifferentType_allowsInsert`
- `addCategory_emptyName_rejectsBeforeInsert`
- `addCategory_nameLongerThan50Chars_rejectsBeforeInsert`
- `updateCategory_ownedByCurrentUser_updatesAndReturnsSuccess`
- `updateCategory_systemCategory_rejectsBeforeUpdate`
- `updateCategory_ownedByAnotherUser_rejectsBeforeUpdate`
- `updateCategory_emptyName_rejectsBeforeUpdate`
- `updateCategory_nameLongerThan50Chars_rejectsBeforeUpdate`
- `deleteCategory_ownedByCurrentUser_marksSoftDeleteAndUpdates`
- `deleteCategory_systemCategory_rejectsBeforeUpdate`
- `updateCategory_duplicateNameAndTypeWithSystemCategory_rejectsBeforeUpdate`
- `updateCategory_sameNameDifferentTypeFromSystemCategory_allowsUpdate`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/goal/CalculateGoalRequiredMonthlyUseCaseTest.java` (6)

- `execute_normalCase_dividesRemainingByMonthsRemaining`
- `execute_afterIncreasingCurrentAmount_requiredMonthlyDecreasesImmediately`
- `execute_targetEqualsCurrent_returnsAchievedWithZeroAmount`
- `execute_currentExceedsTarget_returnsExceeded`
- `execute_noMonthsRemainingAndNotAchieved_returnsDueNowWithFullRemaining`
- `execute_remainingNotDivisibleByMonths_roundsUp`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/goal/GoalStatusUseCaseTest.java` (6)

- `execute_currentBelowTargetAndDeadlineInFuture_returnsActive`
- `execute_currentEqualsTarget_returnsAchieved`
- `execute_currentExceedsTarget_returnsAchieved`
- `execute_deadlinePassedAndNotAchieved_returnsExpired`
- `execute_deadlinePassedButAchieved_stillReturnsAchieved`
- `execute_currentReducedBelowTargetAfterAchieved_returnsActiveAgain`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/goal/GoalUseCaseTest.java` (2)

- `deleteGoal_ownedByCurrentUser_softDeletesAndReturnsSuccess`
- `deleteGoal_ownedByAnotherUser_rejectsBeforeUpdate`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/transaction/AddSuggestedTransactionsUseCaseTest.java` (4)

- `execute_allValid_savesEveryEntityInOrderAndReturnsCount`
- `execute_oneInvalidEntity_savesNothingAndReturnsError`
- `execute_emptyList_returnsSuccessWithZeroAndSavesNothing`
- `execute_repositoryFailsOnSecondInsert_stopsAndReturnsThatError`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/transaction/AddTransactionUseCaseTest.java` (3)

- `execute_validEntity_insertsAndReturnsSuccess`
- `execute_amountZero_rejectsBeforeReachingRepository`
- `execute_noteTooLong_rejectsBeforeReachingRepository`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/transaction/CheckTransactionAnomalyUseCaseTest.java` (6)

- `high_withOtherPending_setsPendingFlag`
- `low_withoutOtherPending_noPendingFlag`
- `normal_doesNotQueryPending`
- `insufficientData_doesNotQueryPending`
- `pendingCountFails_stillReturnsResultWithoutFlag`
- `apiError_isPropagated`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/transaction/DeleteTransactionUseCaseTest.java` (1)

- `execute_marksSoftDeleteFieldsAndUpdates`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/transaction/FilterTransactionsUseCaseTest.java` (5)

- `execute_noFilters_returnsAllNonDeletedTransactions`
- `execute_filterByCategory_returnsOnlyMatchingCategoryTransactions`
- `execute_filterByDateRange_returnsOnlyTransactionsWithinRange`
- `execute_filterByCategoryAndDateRange_returnsIntersectionOfBothConditions`
- `execute_alwaysExcludesSoftDeletedTransactions_regardlessOfOtherFilters`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/transaction/FindSimilarTransactionUseCaseTest.java` (6)

- `execute_passesAmountCategoryAndWholeDayRangeToRepository`
- `execute_justBeforeMidnight_stillUsesSameDay`
- `execute_repositoryFindsSimilar_returnsThatTransaction`
- `execute_noSimilarTransaction_returnsSuccessWithNull`
- `execute_repositoryError_passesErrorThrough`
- `execute_nullCategory_returnsNullWithoutQueryingRepository`

### `android/app/src/test/java/com/longvuong/plix/domain/usecase/transaction/UpdateTransactionUseCaseTest.java` (2)

- `execute_validEntity_setsUpdatedAtAndPendingStatus`
- `execute_invalidAmount_rejectsBeforeReachingRepository`

### `android/app/src/test/java/com/longvuong/plix/domain/validation/FormValidatorTest.java` (28)

- `validateAmount_zero_returnsError`
- `validateAmount_negative_returnsError`
- `validateAmount_overMaxLimit_returnsError`
- `validateAmount_validValue_returnsSuccess`
- `validateAmount_exactlyMaxLimit_returnsSuccess`
- `validateNote_exceeds500Characters_returnsError`
- `validateNote_exactly500Characters_returnsSuccess`
- `validateNote_null_returnsSuccess`
- `validateOccurredAt_tooFarInFuture_returnsError`
- `validateOccurredAt_tooFarInPast_returnsError`
- `validateOccurredAt_today_returnsSuccess`
- `validatePeriod_correctFormat_returnsSuccess`
- `validatePeriod_invalidMonth_returnsError`
- `validatePeriod_wrongOrderFormat_returnsError`
- `validatePeriod_null_returnsError`
- `validateGoalCurrentAmount_negative_returnsError`
- `validateGoalCurrentAmount_zero_returnsSuccess`
- `validateGoalCurrentAmount_overMaxLimit_returnsError`
- `validateGoalCurrentAmount_validValue_returnsSuccess`
- `validateGoalName_empty_returnsError`
- `validateGoalName_blank_returnsError`
- `validateGoalName_null_returnsError`
- `validateGoalName_validValue_returnsSuccess`
- `validateGoalName_exceedsMaxLength_returnsError`
- `validateGoalName_exactlyMaxLength_returnsSuccess`
- `validateGoalDeadline_pastDate_returnsError`
- `validateGoalDeadline_today_returnsSuccess`
- `validateGoalDeadline_futureDate_returnsSuccess`

### `android/app/src/test/java/com/longvuong/plix/presentation/analytics/AnalyticsViewModelTest.java` (21)

- `formatRangeLabel_month`
- `formatRangeLabel_week`
- `formatRangeLabel_threeMonths`
- `formatRangeLabel_sixMonths`
- `rangeLabel_initially_isCurrentMonth`
- `setRangePreset_updatesSelectedPresetAndLabel`
- `state_beforeAnySourceLoaded_isLoading`
- `state_transactionsLoadedButCategoriesNot_staysLoading`
- `state_setRangePresetBeforeDataLoaded_staysLoading`
- `state_noTransactions_isEmpty`
- `state_onlyTransactionsOutsideRange_isEmptyUntilRangeWidened`
- `success_categorySlices_useNamesPercentAndColorOrder`
- `success_unknownOrMissingCategory_mergedIntoUncategorized`
- `success_exactlyFiveCategories_allShownWithoutGrouping`
- `success_moreThanFiveCategories_groupsTheRestIntoOneSlice`
- `success_onlyIncome_hasNoCategorySlices`
- `success_monthBar_splitsIncomeIntoExpenseAndRemaining`
- `success_monthBar_expenseOverIncome_remainingIsZero`
- `success_threeMonths_monthBarsAreSortedByMonth`
- `success_totalsSavingsRateAndCenterLabel`
- `success_centerLabel_followsPreset`

### `android/app/src/test/java/com/longvuong/plix/presentation/analytics/ForecastViewModelTest.java` (12)

- `init_doesNotCallRepositoryUntilLoadForecast`
- `loadForecast_showsLoadingThenSuccess`
- `loadForecast_success_includesMonthToDateNetSeries`
- `loadForecast_insufficientData_becomesEmptyNotError`
- `loadForecast_error_becomesErrorWithMessage`
- `loadForecast_readingTransactionsFails_becomesErrorWithMessage`
- `loadForecast_whileLoading_doesNotCallRepositoryAgain`
- `loadForecast_afterCompleted_canLoadAgain`
- `onCleared_cancelsPendingForecast`
- `loadForecast_online_showsCard`
- `loadForecast_offline_hidesCardAndDoesNotCallRepository`
- `connectivityChanges_hideCardWhenOfflineAndReloadWhenBackOnline`

### `android/app/src/test/java/com/longvuong/plix/presentation/auth/AuthViewModelTest.java` (3)

- `register_confirmPasswordDiffers_emitsMismatchError`
- `register_confirmPasswordDiffersByCase_emitsMismatchError`
- `register_confirmPasswordEmpty_emitsMismatchError`

### `android/app/src/test/java/com/longvuong/plix/presentation/budget/BudgetDisplayNameTest.java` (4)

- `of_overallBudget_returnsOverallName`
- `of_categoryStillActive_returnsCategoryName`
- `of_categoryDeleted_returnsUnknownCategoryName`
- `periodLabel_formatsMonthAndYear`

### `android/app/src/test/java/com/longvuong/plix/presentation/budget/BudgetProgressLevelTest.java` (4)

- `of_belowThreshold_isNormal`
- `of_atOrAboveThresholdBelow100_isWarning`
- `of_atOrAbove100_isDanger`
- `of_threshold100_reaching100IsDanger`

### `android/app/src/test/java/com/longvuong/plix/presentation/category/CategoryGrouperTest.java` (5)

- `group_emptyList_returnsEmptyList`
- `group_expenseAndIncome_returnsExpenseGroupThenIncomeGroupWithCounts`
- `group_mixedInputOrder_keepsExpenseFirstAndOriginalOrderInsideEachGroup`
- `group_onlyExpense_hasNoIncomeHeader`
- `group_onlyIncome_hasNoExpenseHeader`

### `android/app/src/test/java/com/longvuong/plix/presentation/common/AmountTextWatcherTest.java` (4)

- `parse_ignoresSeparatorsAndLetters`
- `parse_emptyOrNoDigits_returnsZero`
- `parse_tooLargeForLong_returnsZero`
- `format_usesDotAsThousandSeparator`

### `android/app/src/test/java/com/longvuong/plix/presentation/common/CategoryIconMapperTest.java` (4)

- `iconFor_systemExpenseCategories_returnsMatchingIcon`
- `iconFor_systemIncomeCategories_returnsMatchingIcon`
- `iconFor_userCategory_returnsDefaultIcon`
- `iconFor_nullCategoryId_returnsDefaultIcon`

### `android/app/src/test/java/com/longvuong/plix/presentation/common/PaymentMethodUiTest.java` (4)

- `codes_hasFiveMethodsIncludingOther_inDisplayOrder`
- `labelOf_eachMethod_returnsVietnameseLabel`
- `iconOf_eachMethod_returnsMatchingIcon`
- `unknownOrNullCode_hasNoLabelAndFallsBackToOtherIcon`

### `android/app/src/test/java/com/longvuong/plix/presentation/goal/GoalDisplayTextTest.java` (7)

- `statusLabel_returnsDesignLabels`
- `statusColors_matchStatus`
- `requiredMonthlyText_normal_showsMonthlyAmount`
- `requiredMonthlyText_achieved_showsDoneMessage`
- `requiredMonthlyText_exceeded_showsExceededAmount`
- `requiredMonthlyText_dueNowActive_isWarning`
- `requiredMonthlyText_dueNowExpired_isDanger`

### `android/app/src/test/java/com/longvuong/plix/presentation/settings/CategorySummaryTextTest.java` (4)

- `of_systemAndCustomCategories_countsEachGroup`
- `of_onlySystemCategories_showsZeroCustom`
- `of_emptyList_showsZeroForBoth`
- `of_nullList_showsZeroForBoth`

### `android/app/src/test/java/com/longvuong/plix/presentation/transaction/AddEditTransactionViewModelTest.java` (36)

- `addMode_initializesDefaultValues`
- `editMode_loadsExistingTransactionIntoDraftFields`
- `save_invalidAmount_rejectsBeforeReachingRepository`
- `save_validAmount_insertsSuccessfully`
- `categorize_beforeResponseArrives_showsLoading`
- `categorize_successResult_showsCategoryLabelWithConfidence`
- `categorize_lowConfidenceResult_showsLowConfidenceLabel`
- `categorize_errorResult_showsErrorState`
- `connectivityLost_hidesSuggestionBlockEvenIfSuggestionWasShowing`
- `save_anomalyHigh_showsBannerAndKeepsScreenOpen`
- `save_anomalyLow_showsLowBannerWithReminder`
- `save_anomalyWithOtherPending_showsPendingNote`
- `save_anomalyInsufficientData_showsPositiveInfoBannerAndKeepsScreenOpen`
- `save_anomalyInsufficientData_usesFixedMessageEvenWhenServerSendsExplanation`
- `save_anomalyNormal_closesScreenWithoutBanner`
- `save_anomalyApiError_closesScreenSilently`
- `save_offline_skipsAnomalyCheckAndClosesScreen`
- `save_income_skipsAnomalyCheckAndClosesScreen`
- `save_noCategory_skipsAnomalyCheckAndClosesScreen`
- `finishAfterSave_afterInsufficientDataBanner_closesScreen`
- `finishAfterSave_afterHighAnomalyBanner_closesScreen`
- `addMode_recurringOffByDefault_withTodayAsRecurringDay`
- `save_recurringOn_savesTemplateWithMonthlyRule`
- `save_recurringOff_savesRegularTransaction`
- `editMode_recurringTemplate_loadsSwitchAndDay`
- `editMode_templateTurnedOff_clearsRecurrence`
- `editMode_autoGeneratedInstance_exposesSourceNoteAndKeepsRecurrenceFields`
- `similar_sameAmountAndCategory_showsWarningWithDetails`
- `similar_onAnotherDay_mentionsTheDate`
- `similar_noMatch_showsNothing`
- `similar_withoutCategory_doesNotQuery`
- `similar_editMode_neverQueries`
- `similar_amountCleared_hidesWarning`
- `save_success_clearsSimilarWarning`
- `categorize_successResult_exposesCategoryNameAndPercentSeparately`
- `dismissCategorySuggestion_hidesTheCard`

### `android/app/src/test/java/com/longvuong/plix/presentation/transaction/HomeViewModelTest.java` (20)

- `quickAddVisible_whenEnabledAndOnline_isTrue`
- `quickAddVisible_whenDisabledInSettings_isFalse`
- `quickAddVisible_whenOffline_isFalse`
- `quickAddVisible_returnsWhenBackOnline`
- `quickAddVisible_staysFalseWhenBackOnlineButDisabled`
- `homeState_beforeTransactionsLoaded_isLoading`
- `homeState_noTransactions_isEmpty`
- `homeState_summarizesOnlyCurrentMonth`
- `homeState_expenseHigherThanIncome_netBalanceIsNegative`
- `homeState_recentTransactions_keepsFirstFourInGivenOrder`
- `homeState_includesCategoryNames`
- `homeState_noBudgetThisMonth_budgetIsNull`
- `homeState_picksBudgetWithHighestPercent`
- `homeState_budgetBelowThreshold_levelIsNormal`
- `homeState_budgetSpentOverLimit_levelIsDanger`
- `homeState_noBudgetThisMonth_overallRemainingIsNull`
- `homeState_onlyCategoryBudget_overallRemainingIsNull`
- `homeState_overallBudget_remainingIsLimitMinusSpentThisMonth`
- `homeState_overallBudgetSpentOverLimit_remainingIsNegative`
- `homeState_overallBudgetExactlyUsedUp_remainingIsZeroNotNull`

### `android/app/src/test/java/com/longvuong/plix/presentation/transaction/QuickAddNlViewModelTest.java` (39)

- `init_doesNotCallRepositoryUntilSubmit`
- `submit_blankText_becomesErrorAndDoesNotCallRepository`
- `submit_trimsTextAndShowsLoading`
- `submit_success_buildsOneSelectedRowPerItemWithCategoryAndAmount`
- `submit_success_emptyList_becomesEmpty`
- `submit_error_becomesErrorWithMessage`
- `init_aiBusyIsFalse`
- `submit_aiBusyError_setsAiBusyAndParseStateError`
- `submit_networkError_doesNotSetAiBusy`
- `submit_afterAiBusy_resetsAiBusyAndCallsRepositoryAgain`
- `submit_blankTextAfterAiBusy_resetsAiBusy`
- `submit_whileLoading_doesNotCallRepositoryAgain`
- `submit_afterCompleted_canSubmitAgain`
- `submit_lowConfidenceItem_needsCategoryAndIsNotSelected`
- `submit_itemWithoutKnownCategory_needsCategory`
- `submit_itemWithoutAmount_needsAmountAndIsNotSelected`
- `submit_itemWithoutAmountAndCategory_needsBoth`
- `setItemSelected_onItemNeedingAmount_isIgnored`
- `updateItem_onItemWithoutAmount_enteringAmountSelectsRow`
- `setItemSelected_untick_updatesSelectionAndSaveButton`
- `setItemSelected_untickAll_disablesSave`
- `setItemSelected_onItemNeedingCategory_isIgnored`
- `updateItem_changesAmountAndCategoryAndSelectsRow`
- `updateItem_toIncomeCategory_showsPlusSignAndIncomeFlag`
- `validateAmountField_zeroIsRejected_positiveIsAccepted`
- `saveSelected_savesOnlyTickedRowsAsPendingTransactions`
- `saveSelected_exampleSentence_savesTwoTransactionsWithCorrectCategoryAndAmount`
- `saveSelected_editedRowToIncomeCategory_savesWithIncomeType`
- `saveSelected_withoutSession_becomesErrorAndSavesNothing`
- `saveSelected_skipsItemWithoutAmount`
- `saveSelected_invalidAmountInRow_becomesErrorAndSavesNothing`
- `saveSelected_nothingTicked_doesNothing`
- `submit_afterSaveError_clearsOldSaveState`
- `submit_quickAddDisabled_doesNotCallParseNlAndShowsError`
- `submit_offline_doesNotCallParseNlAndShowsError`
- `submit_geminiFallbackOff_sendsAllowGeminiFalse`
- `submit_geminiFallbackOn_sendsAllowGeminiTrue`
- `submit_readsGeminiToggleAtSubmitTime_notAtScreenOpen`
- `getOffline_followsConnectivity`

### `android/app/src/test/java/com/longvuong/plix/presentation/transaction/TransactionGrouperTest.java` (5)

- `group_emptyList_returnsEmptyList`
- `group_sameDay_returnsOneHeaderThenRowsInOriginalOrder`
- `group_todayYesterdayAndOlderDay_usesRelativeTitlesThenDate`
- `group_justBeforeAndJustAfterMidnight_areDifferentDays`
- `group_futureDay_usesDateTitle`

### `android/app/src/test/java/com/longvuong/plix/presentation/transaction/TransactionListViewModelTest.java` (6)

- `transactionListState_whenNoTransactions_isEmpty`
- `transactionListState_whenTransactionsExist_isSuccessWithDateHeadersAndRows`
- `quickAddVisible_whenEnabledAndOnline_isTrue`
- `quickAddVisible_whenDisabledInSettings_isFalse`
- `quickAddVisible_whenOffline_isFalse`
- `quickAddVisible_returnsWhenBackOnline`

## 2. Test instrumented Android (`android/app/src/androidTest`) — 29 test

### `android/app/src/androidTest/java/com/longvuong/plix/core/auth/EncryptedTokenStoreTest.java` (2)

- `saveAndReadTokens_returnsSameValues`
- `clearTokens_removesBothTokens`

### `android/app/src/androidTest/java/com/longvuong/plix/core/network/NetworkObserverTest.java` (1)

- `construct_doesNotCrash_andExposesLiveData`

### `android/app/src/androidTest/java/com/longvuong/plix/data/local/AppDatabaseTest.java` (18)

- `seedCategories_insertsExactly12RowsOnCreate`
- `transaction_insertAndGetById_returnsSameData`
- `budget_insertAndGetById_returnsSameData`
- `goal_insertAndGetById_returnsSameData`
- `correction_insertAndGetById_returnsSameData`
- `category_sameNameDifferentType_bothInsertSuccessfully`
- `category_exactDuplicateNameAndType_throwsConstraintException`
- `transaction_softDeletedAfterUpdate_excludedFromGetAllOnceButStillRetrievableById`
- `category_softDeletedAfterUpdate_excludedFromActiveCategoriesButStillRetrievableById`
- `budget_softDeletedAfterUpdate_excludedFromActiveByPeriodButStillRetrievableById`
- `goal_softDeletedAfterUpdate_excludedFromActiveGoalsButStillRetrievableById`
- `budget_findDeletedCategoryBudget_returnsOnlySoftDeletedRowWithSameKey`
- `allFiveTables_softDeletedPendingRecord_stillReturnedByGetPendingSync`
- `transaction_findSimilar_sameAmountCategoryAndDay_returnsThatTransaction`
- `transaction_findSimilar_differentAmountOrCategory_returnsNull`
- `transaction_findSimilar_dayRangeIncludesStartButExcludesEnd`
- `transaction_findSimilar_softDeletedTransaction_isIgnored`
- `transaction_findSimilar_severalMatches_returnsLatestOne`

### `android/app/src/androidTest/java/com/longvuong/plix/data/local/TransactionListPerformanceTest.java` (3)

- `transactionList_5000Rows_sortedListUnder100ms`
- `transactionList_filterByCategoryAndDateRange_under100ms`
- `transactionList_searchByNote_under100ms`

### `android/app/src/androidTest/java/com/longvuong/plix/data/sync/RecurringTransactionWorkerRoomTest.java` (3)

- `generateMissingInstances_withRealRoomDatabase_insertsExactlyOneInstanceForCurrentPeriod`
- `generateMissingInstances_calledTwiceSameKyWithRealRoomDatabase_doesNotDuplicate`
- `generateMissingInstances_softDeletedTemplateWithRealRoomDatabase_isExcludedByActiveRecurringTemplatesQuery`

### `android/app/src/androidTest/java/com/longvuong/plix/data/sync/SyncPullerRoomTest.java` (2)

- `pull_validBatch_savesAllRecordsAndAdvancesCursorTogether`
- `pull_batchWithCorruptRecord_writesNothingAndKeepsCursor`

## 3. Test backend (`backend/app/tests`, pytest) — 379 test

### `backend/app/tests/test_anomaly_endpoint.py` (4)

- `test_anomaly_returns_flagged_high_with_explanation`
- `test_anomaly_requires_amount`
- `test_anomaly_returns_insufficient_data_for_new_category`
- `test_anomaly_returns_flagged_low_with_iqr_explanation`

### `backend/app/tests/test_anomaly_stats.py` (16)

- `test_amount_above_upper_threshold_is_flagged_high`
- `test_amount_below_lower_threshold_is_flagged_low`
- `test_amount_within_thresholds_is_normal_with_mean_std`
- `test_iqr_amount_above_upper_threshold_is_flagged_high`
- `test_iqr_amount_below_lower_threshold_is_flagged_low`
- `test_iqr_amount_within_thresholds_is_normal`
- `test_nine_samples_still_use_iqr`
- `test_fewer_than_five_samples_is_insufficient_data_for_high_amount`
- `test_fewer_than_five_samples_is_insufficient_data_for_low_amount`
- `test_no_samples_is_insufficient_data`
- `test_wide_spread_iqr_one_dong_is_flagged_low_by_relative_floor`
- `test_wide_spread_iqr_amount_just_below_floor_is_flagged_low`
- `test_wide_spread_iqr_amount_at_floor_or_above_is_normal`
- `test_iqr_with_earlier_huge_outlier_still_flags_one_dong_low`
- `test_mean_std_with_outlier_in_history_one_dong_is_flagged_low`
- `test_relative_floor_does_not_change_high_or_normal_results`

### `backend/app/tests/test_categorize_service.py` (8)

- `test_uses_own_model_when_training_sample_count_at_least_10`
- `test_falls_back_to_seed_model_when_user_has_no_model_yet`
- `test_falls_back_to_seed_model_when_own_model_has_fewer_than_10_samples`
- `test_uses_own_model_exactly_at_threshold_10_samples`
- `test_returns_none_category_id_when_predicted_name_not_in_whitelist`
- `test_raises_when_neither_user_model_nor_seed_model_exists`
- `test_threshold_is_10_per_project_plan`
- `test_falls_back_to_seed_model_when_own_model_row_is_mid_retrain`

### `backend/app/tests/test_category_service.py` (5)

- `test_own_category_is_valid`
- `test_system_category_is_rejected`
- `test_other_user_category_is_rejected`
- `test_case_sensitive_user_id_is_rejected`
- `test_current_user_id_with_stray_whitespace_is_rejected`

### `backend/app/tests/test_correction_endpoint.py` (3)

- `test_correction_is_saved_when_transaction_belongs_to_current_user`
- `test_correction_is_rejected_when_transaction_belongs_to_another_user`
- `test_correction_requires_corrected_category_id`

### `backend/app/tests/test_correction_service.py` (5)

- `test_saves_correction_when_transaction_belongs_to_user`
- `test_allows_missing_predicted_category_id`
- `test_raises_when_transaction_belongs_to_another_user`
- `test_raises_when_transaction_does_not_exist`
- `test_transaction_lookup_query_filters_soft_deleted_transactions`

### `backend/app/tests/test_forecast.py` (17)

- `test_exponential_smoothing_applies_alpha_0_3`
- `test_exponential_smoothing_of_constant_series_is_that_constant`
- `test_weekly_series_is_income_minus_expense_per_week`
- `test_weekly_series_skips_current_week_and_fills_empty_weeks_with_zero`
- `test_weekly_series_ignores_unknown_transaction_type`
- `test_week_boundary_uses_vietnam_time_monday`
- `test_forecast_for_current_month_by_default_uses_actual_plus_remaining_weeks`
- `test_forecast_for_other_months_is_not_the_current_month`
- `test_forecast_ignores_transactions_dated_after_now`
- `test_forecast_counts_income_not_only_expense`
- `test_three_complete_weeks_is_insufficient_data`
- `test_four_complete_weeks_is_enough`
- `test_no_transactions_is_insufficient_data`
- `test_only_current_week_transactions_is_insufficient_data`
- `test_check_forecast_returns_ok_with_amount_and_queries_only_this_users_live_rows`
- `test_check_forecast_defaults_to_current_month_when_month_is_none`
- `test_check_forecast_returns_insufficient_data_without_amount`

### `backend/app/tests/test_forecast_endpoint.py` (4)

- `test_forecast_defaults_to_current_month`
- `test_forecast_with_month_param_computes_that_month`
- `test_forecast_returns_insufficient_data_with_null_amount`
- `test_forecast_rejects_invalid_month_format`

### `backend/app/tests/test_gemini_fallback.py` (28)

- `test_should_call_gemini_when_every_part_is_below_threshold`
- `test_should_not_call_gemini_when_any_part_reaches_threshold`
- `test_should_not_call_gemini_when_confidence_equals_threshold`
- `test_should_not_call_gemini_when_there_is_no_part`
- `test_request_body_has_text_category_names_and_fixed_json_schema`
- `test_extract_response_text_reads_first_candidate_text`
- `test_extract_response_text_returns_none_for_unexpected_shape[payload0]`
- `test_extract_response_text_returns_none_for_unexpected_shape[payload1]`
- `test_extract_response_text_returns_none_for_unexpected_shape[payload2]`
- `test_extract_response_text_returns_none_for_unexpected_shape[payload3]`
- `test_extract_response_text_returns_none_for_unexpected_shape[payload4]`
- `test_extract_response_text_returns_none_for_unexpected_shape[None]`
- `test_extract_response_text_returns_none_for_unexpected_shape[payload6]`
- `test_call_gemini_returns_text_and_sends_key_in_header_only`
- `test_call_gemini_without_api_key_raises_unavailable`
- `test_call_gemini_non_200_raises_unavailable[429]`
- `test_call_gemini_non_200_raises_unavailable[500]`
- `test_call_gemini_non_200_raises_unavailable[503]`
- `test_call_gemini_non_200_raises_unavailable[400]`
- `test_call_gemini_non_200_raises_unavailable[403]`
- `test_call_gemini_non_200_raises_unavailable[404]`
- `test_call_gemini_network_error_raises_unavailable[error0]`
- `test_call_gemini_network_error_raises_unavailable[error1]`
- `test_call_gemini_total_timeout_raises_unavailable`
- `test_call_gemini_non_json_body_raises_unavailable`
- `test_parse_with_gemini_keeps_only_valid_items_from_whitelist`
- `test_parse_with_gemini_returns_empty_when_gemini_gives_no_content`
- `test_parse_with_gemini_propagates_unavailable`

### `backend/app/tests/test_naive_bayes.py` (13)

- `test_predict_matches_hand_computed_category_and_confidence`
- `test_predict_other_category_by_symmetry`
- `test_predict_ignores_out_of_vocabulary_words`
- `test_fit_raises_on_empty_documents`
- `test_fit_raises_on_mismatched_lengths`
- `test_predict_on_real_dataset_matches_main_block_example_grab`
- `test_predict_on_real_dataset_matches_main_block_example_grab_full_note`
- `test_predict_on_real_dataset_matches_main_block_example_health_note`
- `test_predict_on_real_dataset_matches_main_block_example_salary_note`
- `test_predict_on_real_dataset_matches_main_block_example_ambiguous_note`
- `test_predict_self_consistency_accuracy_on_full_dataset`
- `test_predict_generalizes_to_held_out_notes_per_category`
- `test_predict_on_fully_unknown_note_returns_uniform_confidence`

### `backend/app/tests/test_nl_parser.py` (38)

- `test_split_by_comma`
- `test_split_by_va_conjunction`
- `test_split_by_roi_conjunction`
- `test_split_mixed_separators_into_many_parts`
- `test_split_keeps_word_that_merely_starts_with_va`
- `test_split_conjunction_is_case_insensitive`
- `test_split_without_separator_returns_single_part`
- `test_split_drops_empty_parts`
- `test_split_empty_or_blank_text_returns_empty_list`
- `test_extract_money_common_formats[50k-50000]`
- `test_extract_money_common_formats[50K-50000]`
- `test_extract_money_common_formats[50 ngh\xecn-50000]`
- `test_extract_money_common_formats[50 ng\xe0n-50000]`
- `test_extract_money_common_formats[500.000\u0111-500000]`
- `test_extract_money_common_formats[1.500.000\u0111-1500000]`
- `test_extract_money_common_formats[500.000 VND-500000]`
- `test_extract_money_common_formats[50000\u0111-50000]`
- `test_extract_money_common_formats[50000-50000]`
- `test_extract_money_common_formats[35-35]`
- `test_extract_money_common_formats[500.000-500000]`
- `test_extract_money_finds_amount_inside_sentence`
- `test_extract_money_returns_none_when_no_amount`
- `test_extract_money_unit_must_stand_alone`
- `test_extract_money_takes_first_amount_from_left`
- `test_split_then_extract_money_for_each_part`
- `test_remove_money_keeps_the_rest_of_the_part`
- `test_match_category_unambiguous_keyword_has_full_confidence`
- `test_match_category_ambiguous_keyword_lowers_confidence`
- `test_match_category_picks_category_with_highest_total_score`
- `test_match_category_unknown_words_returns_none`
- `test_parse_text_example_sentence_gives_two_items`
- `test_parse_text_splits_three_transactions_in_one_sentence`
- `test_parse_text_part_without_money_needs_manual_entry`
- `test_parse_text_unknown_words_keep_amount_but_no_category`
- `test_parse_text_empty_text_returns_empty_list`
- `test_parse_quick_add_maps_category_name_to_whitelist_id`
- `test_parse_quick_add_category_missing_from_whitelist_gives_none`
- `test_parse_quick_add_system_category_wins_over_same_name_user_category`

### `backend/app/tests/test_parse_nl_endpoint.py` (10)

- `test_parse_nl_example_sentence_returns_two_items`
- `test_parse_nl_part_without_money_is_marked_with_null_amount`
- `test_parse_nl_empty_text_returns_no_items`
- `test_parse_nl_requires_text`
- `test_parse_nl_calls_gemini_when_whole_sentence_has_low_confidence`
- `test_parse_nl_does_not_call_gemini_when_rule_parser_is_confident`
- `test_parse_nl_returns_ai_busy_when_gemini_is_unavailable`
- `test_parse_nl_keeps_rule_result_when_gemini_returns_no_valid_item`
- `test_parse_nl_does_not_call_gemini_when_allow_gemini_is_false`
- `test_parse_nl_does_not_call_gemini_when_allow_gemini_is_omitted`

### `backend/app/tests/test_rate_limit.py` (4)

- `test_allows_first_retrain_when_user_has_no_previous_model`
- `test_blocks_when_elapsed_seconds_less_than_cooldown`
- `test_allows_when_elapsed_seconds_equal_to_cooldown`
- `test_allows_when_elapsed_seconds_greater_than_cooldown`

### `backend/app/tests/test_request_size_limit.py` (2)

- `test_request_within_limit_is_allowed`
- `test_request_over_limit_is_rejected`

### `backend/app/tests/test_retrain_endpoint.py` (3)

- `test_first_retrain_succeeds_and_returns_trained_at_and_sample_count`
- `test_retrain_blocked_by_rate_limit_within_cooldown`
- `test_concurrent_retrain_requests_only_one_succeeds`

### `backend/app/tests/test_retrain_no_data.py` (2)

- `test_retrain_without_any_transaction_raises_and_cleans_up`
- `test_retrain_endpoint_returns_400_when_user_has_no_training_data`

### `backend/app/tests/test_retrain_service.py` (2)

- `test_retrain_success_saves_correct_model_and_sample_count`
- `test_second_concurrent_retrain_is_blocked_by_lock`

### `backend/app/tests/test_retrain_user_isolation.py` (2)

- `test_two_users_retrain_produce_fully_independent_models`
- `test_retraining_second_user_does_not_change_first_users_saved_model`

### `backend/app/tests/test_security.py` (3)

- `test_valid_token_returns_user_id`
- `test_expired_token_raises`
- `test_wrong_signature_raises`

### `backend/app/tests/test_security_jwt_all_endpoints.py` (74)

- `test_security_catalog_covers_every_route_of_the_app`
- `test_valid_token_is_accepted`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/whoami-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/whoami-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/whoami-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/categories-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/categories-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/categories-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/anomaly-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/anomaly-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/anomaly-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/forecast-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/forecast-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/forecast-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/categorize-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/categorize-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/categorize-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/correction-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/correction-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/correction-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/retrain-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/retrain-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/retrain-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/parse-nl-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/parse-nl-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/parse-nl-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/transactions/push-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/transactions/push-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/transactions/push-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/transactions/pull-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/transactions/pull-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/transactions/pull-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/categories/push-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/categories/push-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/categories/push-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/categories/pull-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/categories/pull-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/categories/pull-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/budgets/push-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/budgets/push-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/budgets/push-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/budgets/pull-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/budgets/pull-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/budgets/pull-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/goals/push-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/goals/push-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/goals/push-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/goals/pull-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/goals/pull-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/goals/pull-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/corrections/push-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/corrections/push-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[POST /api/v1/sync/corrections/push-malformed]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/corrections/pull-expired]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/corrections/pull-forged_other_key]`
- `test_bad_token_is_rejected_on_every_endpoint[GET /api/v1/sync/corrections/pull-malformed]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/whoami]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/categories]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/anomaly]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/forecast]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/categorize]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/correction]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/retrain]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/parse-nl]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/sync/transactions/push]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/sync/transactions/pull]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/sync/categories/push]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/sync/categories/pull]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/sync/budgets/push]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/sync/budgets/pull]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/sync/goals/push]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/sync/goals/pull]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[POST /api/v1/sync/corrections/push]`
- `test_missing_authorization_header_is_rejected_on_every_endpoint[GET /api/v1/sync/corrections/pull]`

### `backend/app/tests/test_security_jwt_http.py` (8)

- `test_valid_token_is_accepted`
- `test_malformed_token_is_rejected[not-a-jwt]`
- `test_malformed_token_is_rejected[aaa.bbb.ccc]`
- `test_token_without_sub_is_rejected`
- `test_forged_token_signed_with_other_key_is_rejected`
- `test_forged_token_with_tampered_payload_is_rejected`
- `test_forged_unsigned_token_alg_none_is_rejected`
- `test_forged_token_signed_with_hs256_is_rejected`

### `backend/app/tests/test_security_user_isolation.py` (27)

- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/whoami]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/categories]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/anomaly]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/forecast]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/categorize]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/correction]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/retrain]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/parse-nl]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/sync/transactions/push]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/sync/transactions/pull]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/sync/categories/push]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/sync/categories/pull]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/sync/budgets/push]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/sync/budgets/pull]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/sync/goals/push]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/sync/goals/pull]`
- `test_user_id_sent_by_client_never_reaches_the_database[POST /api/v1/sync/corrections/push]`
- `test_user_id_sent_by_client_never_reaches_the_database[GET /api/v1/sync/corrections/pull]`
- `test_whoami_returns_the_user_of_the_token_not_the_one_sent_by_client`
- `test_categories_returns_system_and_own_categories_only`
- `test_categories_ignores_user_id_sent_in_query`
- `test_anomaly_uses_only_the_callers_own_transactions`
- `test_forecast_uses_only_the_callers_own_transactions`
- `test_parse_nl_maps_categories_only_from_the_callers_own_whitelist`
- `test_categorize_returns_only_category_ids_the_caller_can_see`
- `test_correction_on_transaction_of_another_user_is_rejected`
- `test_correction_on_own_transaction_is_saved_for_the_caller`

### `backend/app/tests/test_seed_dataset.py` (3)

- `test_seed_and_holdout_use_the_same_categories`
- `test_seed_has_no_duplicate_notes`
- `test_seed_does_not_overlap_with_holdout`

### `backend/app/tests/test_sync_all_tables.py` (9)

- `test_push_every_table_returns_upserted_ids_in_contract_shape`
- `test_push_every_table_ignores_user_id_sent_by_client_and_uses_jwt`
- `test_push_every_table_rejects_record_missing_a_required_field`
- `test_pull_every_table_returns_records_and_has_more_in_contract_shape`
- `test_pull_every_table_reads_its_own_table_for_the_jwt_user`
- `test_table_outside_the_five_synced_tables_has_no_route`
- `test_push_category_of_another_user_is_rejected_through_the_endpoint`
- `test_push_system_category_is_rejected_through_the_endpoint`
- `test_push_category_batch_with_one_forbidden_record_still_upserts_the_rest`

### `backend/app/tests/test_sync_categories_write.py` (7)

- `test_new_category_is_created_for_the_jwt_user`
- `test_category_upsert_receives_user_id_from_argument_and_fields_in_order`
- `test_own_existing_category_is_upserted`
- `test_own_category_skipped_by_tie_breaker_is_still_reported_as_upserted`
- `test_system_category_is_rejected_without_attempting_upsert`
- `test_category_of_another_user_is_rejected_without_attempting_upsert`
- `test_rejected_category_does_not_block_other_records_in_the_same_batch`

### `backend/app/tests/test_sync_endpoint.py` (10)

- `test_push_returns_upserted_ids_in_contract_shape`
- `test_user_id_sent_by_client_is_ignored_and_jwt_user_id_is_used`
- `test_push_rejects_record_missing_required_field`
- `test_pull_returns_records_and_has_more_in_contract_shape`
- `test_pull_returns_tombstone_records`
- `test_pull_uses_jwt_user_id_and_ignores_user_id_sent_by_client`
- `test_pull_default_limit_is_500_capped_by_max_pull_limit`
- `test_pull_limit_larger_than_max_pull_limit_is_capped`
- `test_pull_rejects_missing_since`
- `test_pull_rejects_limit_below_one`

### `backend/app/tests/test_sync_expired_token.py` (2)

- `test_push_with_expired_token_gets_401_then_retry_with_fresh_token_succeeds`
- `test_pull_with_expired_token_gets_401_then_retry_with_fresh_token_succeeds`

### `backend/app/tests/test_sync_service.py` (25)

- `test_record_written_by_upsert_is_reported_as_upserted`
- `test_upsert_receives_user_id_from_argument_and_record_fields_in_order`
- `test_record_skipped_by_tie_breaker_but_owned_by_same_user_is_still_upserted`
- `test_record_whose_id_belongs_to_another_user_is_rejected_as_forbidden`
- `test_forbidden_record_does_not_block_other_records_in_the_same_batch`
- `test_upsert_sql_uses_strict_greater_than_and_never_deletes`
- `test_pull_returns_rows_as_records_in_the_same_order`
- `test_pull_passes_user_id_since_and_limit_to_the_query`
- `test_pull_includes_tombstone_records`
- `test_pull_with_no_rows_returns_empty_and_skips_has_more_query`
- `test_has_more_is_checked_against_max_updated_at_of_the_batch`
- `test_has_more_is_false_when_nothing_newer_remains`
- `test_pull_sql_uses_gte_orders_deterministically_and_never_filters_tombstones`
- `test_has_more_sql_uses_strict_greater_than_and_filters_by_user`
- `test_sync_tables_cover_exactly_the_five_synced_tables`
- `test_every_table_upsert_sql_is_owner_guarded_strict_and_never_deletes`
- `test_every_table_pull_sql_filters_by_user_and_never_filters_tombstones`
- `test_every_table_owner_sql_looks_up_user_id_by_record_id`
- `test_budget_upsert_receives_user_id_and_record_fields_in_order`
- `test_goal_upsert_receives_user_id_and_record_fields_in_order`
- `test_correction_upsert_receives_user_id_and_record_fields_in_order`
- `test_budget_owned_by_another_user_is_rejected_as_forbidden`
- `test_pull_reads_from_the_requested_table_and_keeps_its_own_columns`
- `test_every_table_upsert_sql_copies_is_deleted_and_updated_at_without_special_rules`
- `test_sync_service_and_router_never_issue_a_physical_delete`

### `backend/app/tests/test_sync_tombstone_duplicate.py` (7)

- `test_tombstone_push_keeps_the_row_and_marks_it_deleted_for_every_table`
- `test_tombstone_of_a_record_never_pushed_before_is_stored_as_tombstone`
- `test_pushing_the_same_payload_twice_is_idempotent_for_every_table`
- `test_older_or_equal_live_payload_cannot_resurrect_a_tombstone_for_every_table`
- `test_user_cannot_tombstone_a_record_owned_by_another_user_for_every_table`
- `test_pull_returns_the_tombstone_after_a_delete_push_for_every_table`
- `test_transaction_pointing_to_a_tombstoned_category_is_still_accepted_and_pulled`

### `backend/app/tests/test_tfidf.py` (11)

- `test_transform_matches_hand_computed_tfidf`
- `test_transform_weighs_repeated_words_higher`
- `test_transform_ignores_out_of_vocabulary_words`
- `test_transform_empty_text_returns_empty_vector`
- `test_fit_raises_on_empty_documents`
- `test_transform_on_real_dataset_matches_main_block_example_grab_note`
- `test_transform_on_real_dataset_matches_main_block_example_health_note`
- `test_transform_on_real_dataset_matches_main_block_example_salary_note`
- `test_transform_on_real_dataset_skips_unknown_words_in_note`
- `test_transform_on_real_dataset_returns_empty_vector_for_unknown_note`
- `test_fit_on_real_dataset_is_deterministic`

### `backend/app/tests/test_validation_error_format.py` (1)

- `test_missing_field_returns_shared_error_response_format`

### `backend/app/tests/test_validation_service.py` (26)

- `test_valid_items_are_kept_and_category_name_is_mapped_to_id`
- `test_item_with_category_outside_whitelist_is_dropped`
- `test_item_with_invalid_amount_is_dropped["50k"]`
- `test_item_with_invalid_amount_is_dropped[0]`
- `test_item_with_invalid_amount_is_dropped[-5000]`
- `test_item_with_invalid_amount_is_dropped[true]`
- `test_item_with_invalid_amount_is_dropped[50000.5]`
- `test_item_with_invalid_amount_is_dropped[null]`
- `test_item_with_invalid_confidence_is_dropped["cao"]`
- `test_item_with_invalid_confidence_is_dropped[1.5]`
- `test_item_with_invalid_confidence_is_dropped[-0.1]`
- `test_item_with_invalid_confidence_is_dropped[true]`
- `test_item_with_invalid_confidence_is_dropped[null]`
- `test_confidence_boundaries_zero_and_one_are_accepted`
- `test_item_missing_a_required_field_is_dropped`
- `test_item_that_is_not_an_object_is_dropped`
- `test_empty_items_list_returns_empty_list`
- `test_malformed_or_wrong_shape_returns_empty_list[None]`
- `test_malformed_or_wrong_shape_returns_empty_list[]`
- `test_malformed_or_wrong_shape_returns_empty_list[kh\xf4ng ph\u1ea3i JSON]`
- `test_malformed_or_wrong_shape_returns_empty_list[{"items": []`
- `test_malformed_or_wrong_shape_returns_empty_list[[]]`
- `test_malformed_or_wrong_shape_returns_empty_list["abc"]`
- `test_malformed_or_wrong_shape_returns_empty_list[{}]`
- `test_malformed_or_wrong_shape_returns_empty_list[{"items": "x"}]`
- `test_malformed_or_wrong_shape_returns_empty_list[{"items": null}]`
