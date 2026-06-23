package flare.client.app.ui.i18n

interface JournalStrings {
    val journal_clear: String
    val journal_title: String
    val journal_waiting_logs: String
    val journal_copy_success: String
    val log_decoding_fragmentation: String
    val log_decoding_mtu_stack: String
    val log_decoding_tunnel_creation: String
}

object RuJournalStrings : JournalStrings {
    override val journal_clear : String = "Очистить"
    override val journal_title : String = "Журнал событий"
    override val journal_waiting_logs : String = "Ожидание новых событий…"
    override val journal_copy_success : String = "События успешно скопированы в буфер обмена!"
    override val log_decoding_fragmentation : String = "Фрагментация включена"
    override val log_decoding_mtu_stack : String = "MTU %1\$s, STACK %2\$s"
    override val log_decoding_tunnel_creation : String = "Создание туннеля..."
}

object EnJournalStrings : JournalStrings {
    override val journal_clear : String = "Clear"
    override val journal_title : String = "Event Journal"
    override val journal_waiting_logs : String = "Waiting for new events…"
    override val journal_copy_success : String = "Events successfully copied to clipboard!"
    override val log_decoding_fragmentation : String = "Fragmentation enabled"
    override val log_decoding_mtu_stack : String = "MTU %1\$s, STACK %2\$s"
    override val log_decoding_tunnel_creation : String = "Creating tunnel..."
}
