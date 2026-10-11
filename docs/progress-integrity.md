# Progress integrity audit

Scope: GameViewModel, repository writes, Room persistence, cloud/local backups and production.

## Confirmed defects and corrections

- License, VIP, guild, tutorial, capsule and researcher actions captured a Flow snapshot before launching work. A later full write could erase intervening income, ad rewards or battle progress. These actions now read within Room transactions (ProgressActions).
- Capsule/card purchases and mission/rank rewards wrote several tables separately. Debit, benefit, cards and mission flags now commit together; callbacks execute after commit. Rank checks all three claimed missions; repeated VIP activation cannot repeat its initial gift. Unknown capsule/research identifiers cannot grant free benefits.
- Market sales now apply science bonuses from the state read in the sale transaction. Autosell activation checks the current license in the repository.
- Offline production aggregated an entire interval per factory. Multiple consumers sharing an input could monopolize its entire supply differently from live ticks. ProductionEngine uses the existing one-second order and surplus rule for both modes. Manual output now includes the same science/card bonuses.
- A backwards clock moved lastSavedTime backwards, reopening an already credited interval. Offline processing leaves that watermark unchanged; online ticks reject times before it and sub-second duplicate ticks. Offline work is capped at 8/12 hours plus Stellar Lab (maximum 12/16 hours), with cancellation checks. No negative or nonfinite balance is committed.
- Backups previously silently truncated fractional integer fields and defaulted missing data; entity IDs and many balances were insufficiently validated. Validation now precedes all writes and rejects invalid fields, duplicate/unknown IDs, incomplete tables, nonfinite quantities, invalid levels and inconsistent lab balances. The transaction rolls back on failure.
- Valid cloud restore closes combat and reconciles transient notifications/event/guild simulation. All database replacement is atomic with production. Pending battle tickets are abandoned, so a previous battle cannot pay after restore. Download cancellation is propagated.
- Demo backup feedback explicitly says local demonstration instead of claiming a Firebase backup.

## Compatibility matrix

| Format | Handling |
|---|---|
| Room 11 | Unchanged schema; no migration needed in this commit. Existing progress retained. |
| Room 10 | Existing 10 -> 11 migration retained (battle tickets). |
| Room 9 | Existing 9 -> 10 -> 11 retained; available cores initialized once from historical cores. |
| Room 8 | Existing 8 -> 9 -> 10 -> 11 retained. |
| Other Room versions | Existing fail-without-deletion behavior retained. No destructive fallback. |
| Backup 4 (new) | All game-state fields, resources, factories, units, researchers, missions, Stellar Lab and paid-ad receipts. |
| Backup 3 | Same progress including Stellar Lab; receipts absent. Local receipts are retained. |
| Backup 2 | Same previously supported legacy progress; available cores = historical cores, lab levels zero. Replacement is not an additive grant. Local receipts retained. |
| Missing version / 1 / future versions | Rejected, as before. |
| Corrupt/incomplete 2/3/4 | Rejected intact rather than silently substituting default progress. This deliberately tightens acceptance of malformed payloads. |

Ad receipts are merged, never used to pay a reward, and never removed during restore. Old backups cannot reconstruct receipts they never contained. Battle tickets and in-memory replay are operational state, not resumable cloud battles; the resulting balances and sector counters are backed up. A restored snapshot starts its offline watermark at restore time, so repeatedly restoring it does not farm an old offline interval.

## Production rules and limitations

- Equivalent online/offline tests use fixed base market prices. Live market prices fluctuate; offline prices retain the previous base-price policy. The game does not persist market history, so fluctuating-price cash parity is not promised.
- Factory processing is ascending ID; input is consumed before output, and autosell sells only the new surplus left after downstream consumers. Passive workers use researcher 5 and science bonuses, without consuming workers.
- Seasonal/guild simulated activity was already memory-only. This audit does not make those simulations persistent; real event reward claims and currencies are persisted. Restore resets transient simulation counters to avoid eligibility inherited from the replaced save.
- Clock changes cannot be authenticated locally. The cap limits forward jumps; after a backward jump, production waits for the watermark to be reached. No server-time or anti-cheat service was introduced.
- Paid time-warp remains the existing supply/cash bundle formula, not elapsed offline time; it is charged atomically.
- Cloud snapshots replace progress; they do not merge independent games from two devices. Firebase security rules/server authority, real billing and Store publication remain separate work.
- Ad receipts grow with rewarded ads. Long-term Firestore document sizing/archival remains a production concern.
- Physical-device checks still required: app background/resume and clock changes, long offline calculation on a slow phone, Firebase restore while production is active, real ad dismissal during restore. No device validation is claimed here.

## Verification

See final task report for actual build, lint and test results. Added tests cover simultaneous purchases/production/ads, unique mission/rank/VIP grants, rollback, malformed payload preservation, backup 2/3/4 and receipts, and fixed-price online/offline equivalence. Existing migration, Stellar Lab, combat, ad and launch regressions remain in the suite.

Final verification: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` completed successfully (1m18s final run). 57 tests, zero failures/errors/skipped; lint zero errors and 77 existing warnings. Two old Stellar Lab fixtures were corrected to include the historical cores required by their upgrades. Numeric range validation uses API-24-compatible comparisons. The combat concurrency test now freezes virtual time while real threads contend, preventing an unrelated virtual defeat during the assertion. No combat implementation change was needed during final verification.
