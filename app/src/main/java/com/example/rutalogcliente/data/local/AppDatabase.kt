package com.example.rutalogcliente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rutalogcliente.data.local.dao.EnvioDao
import com.example.rutalogcliente.data.local.dao.OperacionPendienteDao
import com.example.rutalogcliente.data.local.dao.UsuarioDao
import com.example.rutalogcliente.data.local.entities.EnvioEntity
import com.example.rutalogcliente.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcliente.data.local.entities.SyncMetadataEntity
import com.example.rutalogcliente.data.local.entities.UsuarioEntity

/**
 * Base de datos local (SQLite mediante Room).
 * Tablas: usuarios, envios, operaciones_pendientes y sync_metadata.
 * Los envíos ya no se crean de ejemplo: se descargan de la API REST (RF11).
 */
@Database(
    entities = [
        UsuarioEntity::class,
        EnvioEntity::class,
        OperacionPendienteEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun envioDao(): EnvioDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getDB(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rutalog_cliente.db"
                )
                    // La versión 1 no tenía los campos de sincronización: se recrea la base.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
