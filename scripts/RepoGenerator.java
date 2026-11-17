import java.io.*;
import java.nio.file.*;
import java.util.List;

public class RepoGenerator {

    private static final String ENTITY_PATH = "src/main/java/com/blue/open/Code/Entity/Db";
    private static final String REPO_PATH = "src/main/java/com/blue/open/Code/Repository";
    private static final String CUSTOM_PATH = REPO_PATH + "/Custom";
    private static final String IMPL_PATH = REPO_PATH + "/Impl";

    public static void main(String[] args) throws IOException {
        System.out.println("==== INICIO DE EJECUCIÓN RepoGenerator ====");

        Files.createDirectories(Paths.get(REPO_PATH));
        Files.createDirectories(Paths.get(CUSTOM_PATH));
        Files.createDirectories(Paths.get(IMPL_PATH));

        File carpeta = new File(ENTITY_PATH);
        File[] archivos = carpeta.listFiles((dir, name) -> name.endsWith(".java"));

        if (archivos == null || archivos.length == 0) {
            System.out.println("No se encontraron entidades en: " + carpeta.getAbsolutePath());
            return;
        }

        System.out.println("Cantidad total de archivos .java encontrados: " + archivos.length);

        int totalRepositorios = 0;
        int totalIgnorados = 0;

        for (File archivo : archivos) {
            String nombreEntidad = archivo.getName().replace(".java", "");
            System.out.println("Procesando archivo: " + archivo.getName());

            List<String> lineas = Files.readAllLines(archivo.toPath());

            boolean esCompleta = lineas.stream().anyMatch(l -> l.contains("AbstractEntity"));
            boolean esSimple = lineas.stream().anyMatch(l -> l.contains("AbstractEntitySimple"));
            boolean esConstante = lineas.stream().anyMatch(l -> l.contains("AbstractEntityConstante"));

            if (!esCompleta && !esSimple && !esConstante) {
                System.out.println(">>> Entidad ignorada: " + nombreEntidad);
                totalIgnorados++;
                continue;
            }

            if (esCompleta) System.out.println(">>> Tipo de entidad: COMPLETE");
            if (esSimple) System.out.println(">>> Tipo de entidad: SIMPLE");
            if (esConstante) System.out.println(">>> Tipo de entidad: CONSTANTE");

            Path archivoRepo = Paths.get(REPO_PATH, nombreEntidad + "Repository.java");
            if (!Files.exists(archivoRepo)) {
                Files.writeString(archivoRepo, generarRepositorio(nombreEntidad, esCompleta, esSimple, esConstante));
                System.out.println(">>> Repositorio generado: " + archivoRepo);
                totalRepositorios++;
            } else {
                System.out.println(">>> Repositorio ya existe: " + archivoRepo);
            }

            if (esCompleta) {
                Path archivoCustom = Paths.get(CUSTOM_PATH, nombreEntidad + "RepositoryCustom.java");
                Path archivoImpl = Paths.get(IMPL_PATH, nombreEntidad + "RepositoryImpl.java");

                if (!Files.exists(archivoCustom)) {
                    Files.writeString(archivoCustom, generateCustomContent(nombreEntidad));
                    System.out.println(">>> Custom generado: " + archivoCustom);
                    totalRepositorios++;
                } else {
                    System.out.println(">>> Custom ya existe: " + archivoCustom);
                }

                if (!Files.exists(archivoImpl)) {
                    Files.writeString(archivoImpl, generateImplContent(nombreEntidad));
                    System.out.println(">>> Impl generado: " + archivoImpl);
                    totalRepositorios++;
                } else {
                    System.out.println(">>> Impl ya existe: " + archivoImpl);
                }
            }
        }

        System.out.println("Cantidad de entidades ignoradas: " + totalIgnorados);
        System.out.println("Cantidad total de repositorios creados: " + totalRepositorios);
        System.out.println("==== FIN DE EJECUCIÓN RepoGenerator ====");
    }

    private static String generarRepositorio(String entidad, boolean esCompleta, boolean esSimple, boolean esConstante) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.blue.open.Code.Repository;\n\n");
        sb.append("import com.blue.open.Code.Entity.Db.").append(entidad).append(";\n");

        if (esCompleta) {
            sb.append("import com.blue.open.Core.Repository.Custom.PaginateAutomatico;\n");
            sb.append("import com.blue.open.Core.Repository.EntityRepository;\n\n");
            sb.append("public interface ").append(entidad).append("Repository extends EntityRepository<").append(entidad).append(">, PaginateAutomatico<").append(entidad).append("> {\n");
        } else if (esSimple) {
            sb.append("import com.blue.open.Core.Repository.EntityRepositorySimple;\n\n");
            sb.append("public interface ").append(entidad).append("Repository extends EntityRepositorySimple<").append(entidad).append("> {\n");
        } else if (esConstante) {
            sb.append("import com.blue.open.Core.Repository.EntityRepositoryConstante;\n\n");
            sb.append("public interface ").append(entidad).append("Repository extends EntityRepositoryConstante<").append(entidad).append("> {\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String generateCustomContent(String entidad) {
        return "package com.blue.open.Code.Repository.Custom;\n\n" +
                "import com.blue.open.Code.Entity.Db." + entidad + ";\n" +
                "import com.blue.open.Core.Entity.DTO.PaginateList;\n" +
                "import org.springframework.data.domain.Page;\n\n" +
                "public interface " + entidad + "RepositoryCustom {\n" +
                "    Page<" + entidad + "> paginate(PaginateList paginateList);\n" +
                "}\n";
    }

    private static String generateImplContent(String entidad) {
        return "package com.blue.open.Code.Repository.Impl;\n\n" +
                "import com.blue.open.Code.Entity.Db." + entidad + ";\n" +
                "import com.blue.open.Code.Repository.Custom." + entidad + "RepositoryCustom;\n" +
                "import com.blue.open.Core.Entity.DTO.PaginateList;\n" +
                "import com.blue.open.Core.Repository.Impl.RepositoryImpl;\n" +
                "import jakarta.transaction.Transactional;\n" +
                "import org.springframework.data.domain.Page;\n\n" +
                "public class " + entidad + "RepositoryImpl extends RepositoryImpl<" + entidad + "> implements " + entidad + "RepositoryCustom {\n" +
                "    @Override\n" +
                "    @Transactional\n" +
                "    public Page<" + entidad + "> paginate(PaginateList paginateList) {\n" +
                "        return super.createSql(paginateList);\n" +
                "    }\n" +
                "}\n";
    }
}
