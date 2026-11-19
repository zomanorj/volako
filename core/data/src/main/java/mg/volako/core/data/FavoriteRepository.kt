package mg.volako.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mg.volako.core.database.dao.FavoriteDao
import mg.volako.core.database.entity.asEntity
import mg.volako.core.database.entity.asModel
import mg.volako.core.model.Favorite

// Les favoris de l'onglet MVola (stockes sur le telephone, jamais de code secret)
interface FavoriteRepository {
    fun observeFavorites(): Flow<List<Favorite>>
    suspend fun get(id: Long): Favorite?
    suspend fun add(favorite: Favorite): Long
    suspend fun delete(id: Long)
}

class OfflineFavoriteRepository(private val dao: FavoriteDao) : FavoriteRepository {
    override fun observeFavorites(): Flow<List<Favorite>> = dao.observeAll().map { list -> list.map { it.asModel() } }
    override suspend fun get(id: Long): Favorite? = dao.get(id)?.asModel()
    override suspend fun add(favorite: Favorite): Long = dao.insert(favorite.asEntity().copy(id = 0))
    override suspend fun delete(id: Long) = dao.delete(id)
}
