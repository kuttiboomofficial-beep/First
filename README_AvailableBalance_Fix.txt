MoneyMate - Available Balance fix (audit build)

Change made:
- Available Balance now uses all-time income minus all-time expenses, total recorded goal savings, and the monthly EMI commitment.
- The calculation no longer uses the selected dashboard month, so changing the month does not change Available Balance.
- Month-specific Income, Expense, Transactions, budget and dashboard cash-flow details remain month-specific.
- The same overall-balance formula is used in the dashboard snapshot to avoid showing a conflicting balance.

Validation performed:
- Extracted inline JavaScript passed `node --check` syntax validation.

Important before Play Store upload:
- This is an updated HTML file only, not a rebuilt/signed Android AAB.
- Replace the correct HTML asset in the Android project and rebuild/test the app before uploading.
- Confirm the intended balance definition with real data: all-time transaction net - total recorded goal savings - one monthly EMI commitment. If EMI payments are also entered as expense transactions, the current formula may count them twice; review this before release.
- Test with a backup copy first and verify month switching, transaction edits/deletes, goals, EMI, backup/restore, and offline use.
