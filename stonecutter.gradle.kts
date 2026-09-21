plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.147" apply false
}

stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    // Loader constants so sources can branch:  //? if neoforge { ... }
    constants.match(current.project.substringAfterLast('-'), "neoforge", "forge")
}
