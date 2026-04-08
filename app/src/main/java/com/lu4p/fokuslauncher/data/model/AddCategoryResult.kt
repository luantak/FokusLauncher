package com.lu4p.fokuslauncher.data.model

/** Outcome of trying to add a user-defined app category in the app repository. */
sealed interface AddCategoryResult {
    data object Success : AddCategoryResult

    sealed interface Failure : AddCategoryResult {
        data object Blank : Failure

        data object ReservedAllApps : Failure

        data object ReservedUncategorized : Failure

        data object ReservedPrivate : Failure

        data object ReservedWork : Failure

        /** [canonicalName] is the normalized, stored form (e.g. English key after locale mapping). */
        data class Duplicate(val canonicalName: String) : Failure
    }
}
