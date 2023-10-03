val gitSubmodules: List<File> by lazy {
    (rootDir.listFiles()?.toList() ?: emptyList()).filter {
        it.isDirectory && File(it, ".git").isFile
    }
}

fun File.git(vararg args: String, fail: Boolean = true) {
    println("git ${args.joinToString(" ")}")
    try {
        exec {
            workingDir(this@git)
            commandLine("git", *args)
        }
    } catch (e: Throwable) {
        if (fail) throw e
    }
}

tasks {
    val updateRepos by creating {
        doLast {
            for (file in gitSubmodules) {
                println("FILE: $file")

                file.git("pull")
            }
        }
    }
    val updateTemplates by creating {
        doLast {
            val templateSettingsGradleKts = File(rootDir, "_template/settings.gradle.kts.template")
            val template = templateSettingsGradleKts.readText()
            for (file in gitSubmodules) {
                print("FILE: $file...")

                val settingsGradleKts = File(file, "settings.gradle.kts")

                if (File(file, "gradle/libs.versions.toml").takeIf { it.isFile }?.readText()?.contains("korge = { id = \"com.soywiz.korge\", version = \"5.0.") != true) {
                    println("NOT RIGHT TEMPLATE")
                    continue
                }

                if (settingsGradleKts.readText() != template) {
                    settingsGradleKts.writeText(template)
                    file.git("add", "settings.gradle.kts")
                    file.git("commit", "-m", "Updated settings.gradle.kts", fail = false)
                    file.git("push")
                    println("UPDATED")
                } else {
                    println("UP-TO-DATE")
                }
            }
        }
    }
    val makeGradlewExecutable by creating {
        doLast {
            for (file in gitSubmodules) {
                print("FILE: $file...")

                if (File(file, "gradlew").isFile) {
                    file.git("update-index", "--chmod=+x", "gradlew")
                    file.git("commit", "-m", "Make gradlew executable", fail = false)
                    file.git("push")
                    println("DONE")
                } else {
                    println("DO NOT EXISTS")
                }
            }
        }
    }
}
