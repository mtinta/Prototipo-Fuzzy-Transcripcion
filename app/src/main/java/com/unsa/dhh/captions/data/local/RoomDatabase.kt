package com.unsa.dhh.captions.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.local.dao.DictionaryDao
import com.unsa.dhh.captions.data.local.dao.SessionLogDao
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.SessionLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

/**
 * Módulo 4: Persistencia SQLite local mediante Android Jetpack Room DB.
 * Almacena e indexa los diccionarios de dominio académico de educación secundaria (MINEDU)
 * y la telemetría de sesiones para análisis de WER y latencia.
 */
@Database(
    entities = [AcademicTerm::class, SessionLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun dictionaryDao(): DictionaryDao
    abstract fun sessionLogDao(): SessionLogDao

    companion object {
        private const val TAG = "AppRoomDatabase"
        private const val DB_NAME = "dhh_captions_database.db"

        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        fun getDatabase(context: Context): AppRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    DB_NAME
                )
                    .addCallback(DatabasePrepopulateCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabasePrepopulateCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.i(TAG, "Inicializando y pre-poblando Room DB con diccionarios escolares MINEDU...")
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    val dao = database.dictionaryDao()

                    val dictionaryFiles = listOf(
                        "dictionaries/biologia.json",
                        "dictionaries/quimica.json",
                        "dictionaries/historia.json",
                        "dictionaries/geografia.json"
                    )

                    val gson = Gson()
                    val listType = object : TypeToken<List<AcademicTerm>>() {}.type

                    for (file in dictionaryFiles) {
                        try {
                            context.assets.open(file).use { inputStream ->
                                InputStreamReader(inputStream).use { reader ->
                                    val terms: List<AcademicTerm> = gson.fromJson(reader, listType)
                                    dao.insertAll(terms)
                                    Log.d(TAG, "Cargados ${terms.size} términos desde $file")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error cargando archivo de diccionario $file: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
