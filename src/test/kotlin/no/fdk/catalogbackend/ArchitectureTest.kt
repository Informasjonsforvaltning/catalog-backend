package no.fdk.catalogbackend

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RestController

/**
 * Dependency rules for feature-first packaging.
 */
@Tag("unit")
class ArchitectureTest {
    private val productionClasses = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("no.fdk.catalogbackend")

    @Test
    fun `core does not depend on any resource type`() {
        noClasses()
            .that()
            .resideInAPackage("..core..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..resource..")
            .because("core owns mechanism only, a type-specific import here means the next migration has to edit core")
            .check(productionClasses)
    }

    @Test
    fun `resource types do not depend on each other`() {
        slices()
            .matching("no.fdk.catalogbackend.resource.(*)..")
            .should()
            .notDependOnEachOther()
            .because("each resource type must stay independently removable when its source repo is archived")
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    @Test
    fun `only resource types declare tables`() {
        noClasses()
            .that()
            .resideOutsideOfPackage("..resource..")
            .should()
            .beAnnotatedWith(Entity::class.java)
            .orShould()
            .beAnnotatedWith(Table::class.java)
            .because("core declares @MappedSuperclass and owns no table; every table belongs to a resource type")
            .check(productionClasses)
    }

    @Test
    fun `controllers live in core web or in a resource type`() {
        noClasses()
            .that()
            .resideOutsideOfPackages("..core.web..", "..resource..")
            .should()
            .beAnnotatedWith(RestController::class.java)
            .check(productionClasses)
    }

    @Test
    fun `persistence is reached through the store port`() {
        noClasses()
            .that()
            .resideInAPackage("..core.service..")
            .or()
            .resideInAPackage("..core.web..")
            .should()
            .dependOnClassesThat()
            .haveNameMatching(".*Repository")
            .because("core talks to storage through ResourceStore, so repositories stay bound to their own type")
            .check(productionClasses)
    }
}
