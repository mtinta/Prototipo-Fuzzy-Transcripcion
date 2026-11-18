package com.unsa.dhh.captions.data.local.entities

import com.google.gson.annotations.SerializedName

/**
 * Representa una agrupación temática de co-ocurrencia semántica para una asignatura escolar.
 * Modela la probabilidad contextual de que términos pedagógicos aparezcan juntos en una lección.
 */
data class TopicCluster(
    @SerializedName("topic_id")
    val topicId: String = "",
    @SerializedName("topic_name")
    val topicName: String = "",
    /**
     * Área temática que agrupa varios clusters afines (p. ej. "biologia_celular"). Opcional: sin él el
     * cluster solo puntúa por coincidencia exacta. Puede ser null tras la deserialización con Gson.
     */
    @SerializedName("macro_id")
    val macroId: String? = null,
    val terms: List<String> = emptyList()
) {
    // Conjunto en memoria para comprobaciones rápidas durante la inferencia difusa (seguro ante deserialización Gson)
    val termsSet: Set<String>
        get() = (terms ?: emptyList()).map { it.lowercase() }.toSet()
}
