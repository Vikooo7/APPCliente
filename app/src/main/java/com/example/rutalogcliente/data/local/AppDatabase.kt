package com.example.rutalogcliente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun envioDao(): EnvioDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        /**
         * Proyecto final: destinatario, monto contraentrega e intentos en envios. Solo agrega
         * columnas: se conservan los envíos y la cola de operaciones que aún no se enviaron.
         */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `destinatarioNombre` TEXT")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `destinatarioTelefono` TEXT")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `destinatarioDireccion` TEXT")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `montoContraentrega` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `intentos` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Fase de pagos: estado del pago del envío con QR. Los envíos anteriores no lo requieren. */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `pagoEstado` TEXT NOT NULL DEFAULT 'NO_REQUERIDO'")
            }
        }

        fun getDB(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rutalog_cliente.db"
                )
                    .addMigrations(MIGRACION_2_3, MIGRACION_3_4)
                    // La versión 1 no tenía los campos de sincronización: se recrea la base.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
