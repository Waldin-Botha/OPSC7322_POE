package za.ac.iie.opsc_poe_screens

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import androidx.room.*
import java.util.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    var passwordHash: String // TODO Hash password
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val AccountName: String,
    var Balance: Float,
    val Colour: Int,
    val userId: Int
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconId: Int,
    val isIncome: Boolean,
    val userId: Int
)

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var GoalName: String,
    var Description: String,
    var Amount: Int,
    var CurrentAmount: Int,
    var Completed: Boolean,
    var Bonus: Boolean,
    val userId: Int
)

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class ImageItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ImagePath: String,
    val ImageTitle: String,
    val userId: Int
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("categoryId"), Index("userId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Float,
    val description: String,
    val accountId: Int,
    val categoryId: Int,
    val recurring: Boolean,
    val date: Date,
    val receiptPath: String? = null,
    val userId: Int
)

data class TransactionWithAccountAndCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)

