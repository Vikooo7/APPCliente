package com.example.rutalogcliente.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.rutalogcliente.model.CatalogoRutas
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.NumeroGuia
import com.example.rutalogcliente.model.RolUsuario

@Database(entities = [Usuario::class, Envio::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun envioDao(): EnvioDao

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
                    .addCallback(DatosIniciales)
                    .build()
                    .also { instancia = it }
            }
    }
}

/**
 * Datos de ejemplo que se insertan solo la primera vez que se crea la base de datos:
 * una cuenta demo y algunos envíos en distintos estados para la demostración.
 */
private object DatosIniciales : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        db.insert(
            "usuarios",
            SQLiteDatabase.CONFLICT_NONE,
            ContentValues().apply {
                put("nombre", "Distribuidora Andina SAC")
                put("correo", "cliente@rutalog.pe")
                put("clave", "1234")
                put("rol", RolUsuario.CLIENTE.valor)
            }
        )

        val ejemplos = listOf(
            Triple("Lima → Arequipa", 120.5, EstadoEnvio.EN_TRANSITO) to "Carlos Quispe Mamani · AQP-482",
            Triple("Lima → Trujillo", 45.0, EstadoEnvio.ENTREGADO) to "Rosa Huamán Torres · BFK-209",
            Triple("Lima → Cusco", 300.0, EstadoEnvio.RECOGIDO) to "Wilber Condori Apaza · X3C-560",
            Triple("Lima → Iquitos", 80.0, EstadoEnvio.EN_TRANSITO) to "Edwin Tapullima Sangama · U4S-218",
            Triple("Lima → Piura", 15.5, EstadoEnvio.PENDIENTE) to null,
            Triple("Lima → Huancayo", 220.0, EstadoEnvio.EN_REPARTO) to "Milagros Rojas Pérez · F7R-318",
            Triple("Lima → Tacna", 60.0, EstadoEnvio.ENTREGADO) to "Yeni Mamani Choque · Z9P-640",
            Triple("Lima → Chiclayo", 35.0, EstadoEnvio.PENDIENTE) to null
        )

        ejemplos.forEachIndexed { indice, (datos, transportista) ->
            val (ruta, peso, estado) = datos
            val id = indice + 1
            val tarifa = CatalogoRutas.porNombre(ruta)?.tarifaPorKg ?: 0.0
            db.insert(
                "envios",
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put("id", id)
                    put("numeroGuia", NumeroGuia.generar(id))
                    put("ruta", ruta)
                    put("pesoKg", peso)
                    put("costoEnvio", CatalogoRutas.calcularCosto(peso, tarifa))
                    put("estado", estado.codigo)
                    if (transportista == null) putNull("transportistaAsignado") else put("transportistaAsignado", transportista)
                }
            )
        }
    }
}
