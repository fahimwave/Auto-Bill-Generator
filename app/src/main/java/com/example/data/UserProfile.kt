package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "student_id") val studentId: String = "",
    @ColumnInfo(name = "account_no") val accountNo: String = "",
    @ColumnInfo(name = "bank_name") val bankName: String = "MTB",
    @ColumnInfo(name = "signature_path") val signaturePath: String = ""
)
