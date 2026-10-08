package com.walletplus.domain.export

import com.walletplus.domain.model.TransactionFilter

enum class ExportFormat { CSV, PDF, BACKUP }

/**
 * Contrato para exportar datos (CSV, PDF o copia de seguridad).
 * Preparado para una fase posterior; aún no tiene implementación.
 */
interface ExportService {
    /** Devuelve los bytes del archivo generado para que la plataforma decida dónde guardarlo. */
    suspend fun export(format: ExportFormat, filter: TransactionFilter = TransactionFilter()): ByteArray
}
