package mg.monargent.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import mg.monargent.core.database.entity.SmsTemplateEntity

@Dao
interface SmsTemplateDao {

    // les plus recents d'abord : si MVola change encore, le dernier modele appris gagne
    @Query("SELECT * FROM sms_templates ORDER BY created_at DESC")
    suspend fun getAll(): List<SmsTemplateEntity>

    @Insert
    suspend fun insert(template: SmsTemplateEntity): Long
}
