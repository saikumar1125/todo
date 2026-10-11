package com.walltodo

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val doneAt: Long? = null
)

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isDone ASC, createdAt ASC")
    fun observeAll(): Flow<List<Todo>>

    @Query("SELECT * FROM todos ORDER BY isDone ASC, createdAt ASC")
    suspend fun getAll(): List<Todo>

    @Insert suspend fun insert(todo: Todo)
    @Update suspend fun update(todo: Todo)
    @Delete suspend fun delete(todo: Todo)
}

@Database(entities = [Todo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TodoDao
    companion object {
        @Volatile private var inst: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "walltodo.db")
                .build().also { inst = it }
        }
    }
}
