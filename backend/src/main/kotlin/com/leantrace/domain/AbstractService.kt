package com.leantrace.domain
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository
import java.io.Serializable


abstract class AbstractService<D : PagingAndSortingRepository<E, ID>, E : Identifiable<ID>, ID : Serializable>(
    repo: D) {

    var repo: D = repo
        protected set

    fun findAll(page: Int?, size: Int?, sort: List<String>?) =
        if (page != null || size != null) {
            val pageable = pageable(page?:0, size?:20, sort?:emptyList())
            val p = repo.findAll(pageable)
            PageImpl(resolve(p.content.toList()), pageable, p.totalElements)
        }
        else if (sort != null) resolve(repo.findAll(sortable(sort)).toList())
        else resolve(repo.findAll().toList())

    fun findById(id: ID) = repo.findById(id)

    fun deleteById(id: ID) = repo.deleteById(id)

    fun create(entity: E) = repo.save(omit(listOf(entity)).getOrElse(0){entity})

    fun update(id: ID, entity: E): E {
        entity.id = id
        return repo.save(entity)
    }

    private fun pageable(page: Int, size: Int, sort: List<String>) = PageRequest.of(page, size, sortable(sort))

    private fun sortable(sort: List<String>?) =
        Sort.by(sort?.map {
            val (property, direction) = if (it.contains(":")) it.split(':') else listOf(it,"asc")
            if (direction.toLowerCase() == "desc") Sort.Order.desc(property) else Sort.Order.asc(property)
        } ?: emptyList())

    internal fun <T: Identifiable<ID>>resolveObjects(objects: List<Identifiable<ID>>, repo: PagingAndSortingRepository<T, ID>) =
        if (objects.isNotEmpty()) repo.findAllById(objects.map { it.id }.toMutableList().asIterable()).toList()
        else objects
    abstract fun resolve(objects: List<E>): List<E>
    abstract fun omit(objects: List<E>): List<E>
}
