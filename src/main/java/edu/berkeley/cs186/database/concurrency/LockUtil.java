package edu.berkeley.cs186.database.concurrency;

import edu.berkeley.cs186.database.TransactionContext;

/**
 * LockUtil is a declarative layer which simplifies multigranularity lock
 * acquisition for the user (you, in the last task of Part 2). Generally
 * speaking, you should use LockUtil for lock acquisition instead of calling
 * LockContext methods directly.
 */
public class LockUtil {
    /**
     * Ensure that the current transaction can perform actions requiring
     * `requestType` on `lockContext`.
     *
     * `requestType` is guaranteed to be one of: S, X, NL.
     *
     * This method should promote/escalate/acquire as needed, but should only
     * grant the least permissive set of locks needed. We recommend that you
     * think about what to do in each of the following cases:
     * - The current lock type can effectively substitute the requested type
     * - The current lock type is IX and the requested lock is S
     * - The current lock type is an intent lock
     * - None of the above: In this case, consider what values the explicit
     *   lock type can be, and think about how ancestor looks will need to be
     *   acquired or changed.
     *
     * You may find it useful to create a helper method that ensures you have
     * the appropriate locks on all ancestors.
     */
    public static void ensureSufficientLockHeld(LockContext lockContext, LockType requestType) {
        // requestType must be S, X, or NL
        assert (requestType == LockType.S || requestType == LockType.X || requestType == LockType.NL);

        // Do nothing if the transaction or lockContext is null
        TransactionContext transaction = TransactionContext.getTransaction();
        if (transaction == null || lockContext == null) return;

        // You may find these variables useful
        LockContext parentContext = lockContext.parentContext();
        LockType effectiveLockType = lockContext.getEffectiveLockType(transaction);
        LockType explicitLockType = lockContext.getExplicitLockType(transaction);

        if (!LockType.substitutable(effectiveLockType, requestType) && !LockType.substitutable(explicitLockType, requestType)) {
            if (requestType == LockType.NL) {
                return;
            }
            ensureParentLockHeld(parentContext, LockType.parentLock(requestType), transaction);
            if (explicitLockType.equals(LockType.IX) && requestType.equals(LockType.S)) {
                lockContext.promote(transaction, LockType.SIX);
            } else if (explicitLockType.isIntent()) {
                lockContext.escalate(transaction);
            } else {
                if (explicitLockType.equals(LockType.NL)) {
                    lockContext.acquire(transaction, requestType);
                } else {
                    lockContext.promote(transaction, requestType);
                }
            }
        }
    }

    public static void ensureParentLockHeld(LockContext parentContext, LockType requestType, TransactionContext transaction) {
        if (parentContext == null || requestType.equals(LockType.NL)) {
            return;
        }
        LockType effectiveType = parentContext.getEffectiveLockType(transaction);
        LockType explicitType = parentContext.getExplicitLockType(transaction);
        if (requestType.equals(LockType.IX) && explicitType.equals(LockType.S)) {
            requestType = LockType.SIX;
        }
        if (LockType.substitutable(explicitType, requestType) || LockType.substitutable(effectiveType, requestType)) {
            return;
        }
        ensureParentLockHeld(parentContext.parentContext(), LockType.parentLock(requestType), transaction);
        if (explicitType.equals(LockType.NL)) {
            parentContext.acquire(transaction, requestType);
        } else {
            parentContext.promote(transaction, requestType);
        }
    }
}
