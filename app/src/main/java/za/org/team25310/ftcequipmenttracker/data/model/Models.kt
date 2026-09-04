package za.org.team25310.ftcequipmenttracker.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Vendor(val displayName: String) {
    GOBILDA("goBILDA"),
    REV("REV Robotics"),
    STUDICA("Studica"),
    CUSTOM("Custom");

    companion object {
        fun from(value: String?): Vendor =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
    }
}

@Serializable
data class Part(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    val vendor: Vendor = Vendor.CUSTOM,
    val category: String = "",
    val ftcLegal: Boolean = true,
    val aliases: List<String> = emptyList(),
)

data class Team(
    val teamNumber: String = "",
    val name: String = "",
    val city: String = "",
) {
    val displayName: String
        get() = if (name.isNotBlank()) "#$teamNumber $name" else "#$teamNumber"
}

enum class LoanStatus {
    DRAFT,
    PENDING_CONFIRMATION,
    ACTIVE,
    RETURNED,
    OVERDUE,
    CANCELLED;

    companion object {
        fun from(value: String?): LoanStatus =
            entries.firstOrNull { it.name == value } ?: DRAFT
    }
}

data class LoanEvent(
    val id: String = "",
    val partId: String = "",
    val partName: String = "",
    val sku: String = "",
    val vendor: String = "",
    val loanerTeamNumber: String = "",
    val loanerTeamName: String = "",
    val borrowerTeamNumber: String = "",
    val borrowerTeamName: String = "",
    val loanDate: Long = 0L,
    val dueDate: Long = 0L,
    val location: String = "",
    val notes: String = "",
    val photoBase64: String = "",
    val status: LoanStatus = LoanStatus.DRAFT,
    val loanerCoachConfirmed: Boolean = false,
    val borrowerCoachConfirmed: Boolean = false,
    val loanerSignature: String = "",
    val borrowerSignature: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "partId" to partId,
        "partName" to partName,
        "sku" to sku,
        "vendor" to vendor,
        "loanerTeamNumber" to loanerTeamNumber,
        "loanerTeamName" to loanerTeamName,
        "borrowerTeamNumber" to borrowerTeamNumber,
        "borrowerTeamName" to borrowerTeamName,
        "loanDate" to loanDate,
        "dueDate" to dueDate,
        "location" to location,
        "notes" to notes,
        "photoBase64" to photoBase64,
        "status" to status.name,
        "loanerCoachConfirmed" to loanerCoachConfirmed,
        "borrowerCoachConfirmed" to borrowerCoachConfirmed,
        "loanerSignature" to loanerSignature,
        "borrowerSignature" to borrowerSignature,
        "createdBy" to createdBy,
        "createdAt" to createdAt,
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): LoanEvent = LoanEvent(
            id = id,
            partId = map["partId"] as? String ?: "",
            partName = map["partName"] as? String ?: "",
            sku = map["sku"] as? String ?: "",
            vendor = map["vendor"] as? String ?: "",
            loanerTeamNumber = map["loanerTeamNumber"] as? String ?: "",
            loanerTeamName = map["loanerTeamName"] as? String ?: "",
            borrowerTeamNumber = map["borrowerTeamNumber"] as? String ?: "",
            borrowerTeamName = map["borrowerTeamName"] as? String ?: "",
            loanDate = map["loanDate"] as? Long ?: 0L,
            dueDate = map["dueDate"] as? Long ?: 0L,
            location = map["location"] as? String ?: "",
            notes = map["notes"] as? String ?: "",
            photoBase64 = map["photoBase64"] as? String ?: "",
            status = LoanStatus.from(map["status"] as? String),
            loanerCoachConfirmed = map["loanerCoachConfirmed"] as? Boolean ?: false,
            borrowerCoachConfirmed = map["borrowerCoachConfirmed"] as? Boolean ?: false,
            loanerSignature = map["loanerSignature"] as? String ?: "",
            borrowerSignature = map["borrowerSignature"] as? String ?: "",
            createdBy = map["createdBy"] as? String ?: "",
            createdAt = map["createdAt"] as? Long ?: 0L,
        )
    }
}
