package flare.client.app.data.db

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File

class SafeOpenHelperFactory : SupportSQLiteOpenHelper.Factory {
    private val delegate = FrameworkSQLiteOpenHelperFactory()

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val wrappedCallback = object : SupportSQLiteOpenHelper.Callback(configuration.callback.version) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                configuration.callback.onCreate(db)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                configuration.callback.onUpgrade(db, oldVersion, newVersion)
            }

            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                configuration.callback.onDowngrade(db, oldVersion, newVersion)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                configuration.callback.onOpen(db)
            }

            override fun onConfigure(db: SupportSQLiteDatabase) {
                configuration.callback.onConfigure(db)
            }

            override fun onCorruption(db: SupportSQLiteDatabase) {
                Log.e("SafeOpenHelperFactory", "Критическая ошибка: обнаружено повреждение базы данных. Путь: ${db.path}")
                try {
                    val dbPath = db.path
                    if (dbPath != null) {
                        val dbFile = File(dbPath)
                        if (dbFile.exists()) {
                            val backupFile = File(dbPath + ".bak")
                            dbFile.copyTo(backupFile, overwrite = true)
                            Log.i("SafeOpenHelperFactory", "Резервная копия поврежденной БД успешно создана: ${backupFile.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SafeOpenHelperFactory", "Не удалось создать резервную копию базы данных перед удалением", e)
                }
                
                configuration.callback.onCorruption(db)
            }
        }

        val newConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .callback(wrappedCallback)
            .build()

        return delegate.create(newConfig)
    }
}
