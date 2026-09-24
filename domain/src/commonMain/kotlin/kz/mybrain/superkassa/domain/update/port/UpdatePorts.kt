package kz.mybrain.superkassa.domain.update.port

/**
 * Что обновлению кассы нужно снаружи: служба выпусков и память о проверке.
 *
 * @property releases служба выпусков кассы.
 * @property updateMemory что рабочее место помнит о проверке выпусков.
 */
data class UpdatePorts(val releases: Releases, val updateMemory: UpdateMemory)
