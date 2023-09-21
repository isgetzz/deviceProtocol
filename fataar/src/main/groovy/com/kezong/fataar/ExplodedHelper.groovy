package com.kezong.fataar

import org.gradle.api.Project

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * process jars and classes
 */
class ExplodedHelper {

    static void processLibsIntoLibs(Project project,
                                    Collection<AndroidArchiveLibrary> androidLibraries,
                                    Collection<File> jarFiles,
                                    File folderOut) {
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.rootFolder.exists()) {
                FatUtils.logInfo('[warning]' + androidLibrary.rootFolder + ' not found!')
                continue
            }
            if (androidLibrary.localJars.isEmpty()) {
                FatUtils.logInfo("Not found jar file, Library: ${androidLibrary.name}")
            } else {
                FatUtils.logInfo("Merge ${androidLibrary.name} local jar files")
                project.copy {
                    from(androidLibrary.localJars)
                    into(folderOut)
                }
            }
        }
        for (jarFile in jarFiles) {
            if (jarFile.exists()) {
                FatUtils.logInfo("Copy jar from: $jarFile to $folderOut.absolutePath")
                def explodedRootDir = new File(project.getBuildDir(), "/intermediates/exclude-jar/")
                if (!explodedRootDir.exists()) {
                    explodedRootDir.mkdir()
                }
                project.copy {
                    from(jarFile)
                    into(explodedRootDir)
                }
                def tempJar = explodedRootDir.absolutePath + "/" + jarFile.name
//                ZipUnzip.unzip(tempJar, explodedRootDir.absolutePath + "/")

                project.copy {
                    from { project.zipTree(tempJar) }
                    into explodedRootDir
                }

//                def moduleInfoFile = new File(jarTempDir, 'META-INF/versions')
//                println(" moduleInfoFile = "+moduleInfoFile.absolutePath)
//                if (moduleInfoFile.exists()) {
//                    if (moduleInfoFile.isDirectory()) {
//                        moduleInfoFile.deleteDir()
//                    } else {
//                        moduleInfoFile.delete()
//                    }
//                }

                // 重新打包 JAR 文件
//                ZipUnzip.zipFile(jarTempDir.absolutePath, jarTempDir.absolutePath)

                project.copy {
                    from(jarFile)
                    into(folderOut)
                }
            } else {
                FatUtils.logInfo('[warning]' + jarFile + ' not found!')
            }
        }
    }

    static void processClassesJarInfoClasses(Project project,
                                             Collection<AndroidArchiveLibrary> androidLibraries,
                                             File folderOut) {
        FatUtils.logInfo('Merge ClassesJar')
        Collection<File> allJarFiles = new ArrayList<>()
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.rootFolder.exists()) {
                FatUtils.logInfo('[warning]' + androidLibrary.rootFolder + ' not found!')
                continue
            }
            allJarFiles.add(androidLibrary.classesJarFile)
        }
        for (jarFile in allJarFiles) {
            if (!jarFile.exists()) {
                continue
            }
            project.copy {
                from project.zipTree(jarFile)
                into folderOut
                dirMode 0755
                fileMode 0755
            }
        }
    }

    static void processLibsIntoClasses(Project project,
                                       Collection<AndroidArchiveLibrary> androidLibraries,
                                       Collection<File> jarFiles,
                                       File folderOut) {
        FatUtils.logInfo('Merge Libs')
        Collection<File> allJarFiles = new ArrayList<>()
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.rootFolder.exists()) {
                FatUtils.logInfo('[warning]' + androidLibrary.rootFolder + ' not found!')
                continue
            }
            FatUtils.logInfo('[androidLibrary]' + androidLibrary.getName())
            allJarFiles.addAll(androidLibrary.localJars)
        }
        for (jarFile in jarFiles) {
            if (jarFile.exists()) {
                allJarFiles.add(jarFile)
            }
        }
        for (jarFile in allJarFiles) {
            project.copy {
                from project.zipTree(jarFile)
                into folderOut
                exclude 'META-INF/'
                dirMode 0755
                fileMode 0755
            }
        }
    }
    // 解压 ZIP 文件到目录
    static void unzipFile(File zipFile, File destinationDir) {
        def zip = new ZipFile(zipFile)
        zip.entries().each { entry ->
            if (!entry.isDirectory()) {
                def entryPath = destinationDir.toPath().resolve(entry.getName())
                Files.createDirectories(entryPath.getParent())
                Files.copy(zip.getInputStream(entry), entryPath)
            }
        }
        zip.close()
    }

    // 将目录打包为 ZIP 文件
    static void zipFile(File sourceDir, File zipFile) {
        def zip = new ZipOutputStream(new FileOutputStream(zipFile))
        Files.walk(sourceDir.toPath()).forEach { file ->
            if (!Files.isDirectory(file)) {
                def entryName = sourceDir.toPath().relativize(file).toString()
                zip.putNextEntry(new ZipEntry(entryName))
                zip.write(Files.readAllBytes(file))
                zip.closeEntry()
            }
        }
        zip.close()
    }
}
