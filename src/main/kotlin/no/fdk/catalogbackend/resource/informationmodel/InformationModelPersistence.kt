package no.fdk.catalogbackend.resource.informationmodel

import jakarta.persistence.Entity
import jakarta.persistence.Table
import no.fdk.catalogbackend.core.persistence.CatalogResourceEntity
import no.fdk.catalogbackend.core.persistence.CatalogResourceRepository
import no.fdk.catalogbackend.core.persistence.JpaResourceStore
import no.fdk.catalogbackend.core.spi.ResourceTypeMetadata
import org.springframework.stereotype.Component

@Entity
@Table(name = "information_models")
class InformationModelEntity : CatalogResourceEntity()

interface InformationModelRepository : CatalogResourceRepository<InformationModelEntity>

@Component
class InformationModelStore(repository: InformationModelRepository) :
    JpaResourceStore<InformationModelEntity>(INFORMATION_MODEL, repository, InformationModelEntity::class, ::InformationModelEntity)

@Component
class InformationModelMetadata : ResourceTypeMetadata {
    override val resourceType = INFORMATION_MODEL
    override val pathSegment = "information-models"
    override val dataSourceType = "ModellDCAT-AP-NO"
    override val harvestDataType = "informationmodel"
}
