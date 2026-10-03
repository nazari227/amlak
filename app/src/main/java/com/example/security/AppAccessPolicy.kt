package com.example.security

import com.example.domain.model.UserProfile

data class AppAccess(
    val showHome: Boolean = true,
    val showProperties: Boolean = false,
    val showDemands: Boolean = false,
    val showTasks: Boolean = false,
    val showNotifications: Boolean = true,
    val showProfile: Boolean = true,
    val canCreateProperty: Boolean = false,
    val canCreateDemand: Boolean = false,
    val canManageAppointments: Boolean = false,
    val roleLabel: String = "همکار"
)

object AppAccessPolicy {

    fun forUser(user: UserProfile): AppAccess {
        val role = user.role.lowercase().trim()
        val caps = user.capabilities

        fun has(vararg names: String): Boolean = names.any { caps[it] == true }

        val technical = role == "technical"
        val manager = role == "manager"
        val supervisor = role == "supervisor"
        val agent = role == "agent"
        val operator = role == "operator"
        val marketer = role == "marketer"

        // Capabilities are authoritative. Technical admin keeps full access and
        // marketer preserves the current read-oriented mobile experience.
        val showProperties = technical || marketer ||
            has(
                "ia_create_cases",
                "ia_manage_all_cases",
                "ia_manage_branch_cases",
                "ia_manage_assigned_cases"
            )

        val showDemands = technical || marketer ||
            has(
                "ia_create_demands",
                "ia_manage_all_demands",
                "ia_manage_branch_demands"
            )

        // Core 1.1.00 does not expose ia_update_own_tasks in /me capabilities,
        // so staff_type is intentionally used as the safe UI fallback here.
        val showTasks = technical || manager || supervisor || agent || operator ||
            has("ia_manage_tasks", "ia_manage_branch_tasks")

        val roleLabel = when (role) {
            "technical" -> "مدیر فنی سامانه"
            "manager" -> "مدیریت املاک"
            "supervisor" -> "مسئول شعبه"
            "agent" -> "مشاور املاک"
            "operator" -> "اپراتور دفتر"
            "marketer" -> "بازاریاب فایل"
            else -> "همکار"
        }

        return AppAccess(
            showHome = true,
            showProperties = showProperties,
            showDemands = showDemands,
            showTasks = showTasks,
            showNotifications = true,
            showProfile = true,
            canCreateProperty = technical || has("ia_create_cases"),
            canCreateDemand = technical || has("ia_create_demands"),
            canManageAppointments = technical || manager || supervisor ||
                has(
                    "ia_manage_appointments",
                    "ia_manage_branch_appointments",
                    "ia_manage_own_appointments"
                ),
            roleLabel = roleLabel
        )
    }
}
