package com.blue.open.Core.Service;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.DTO.AbstractEntityDTO;
import com.blue.open.Core.Entity.DTO.PaginateFiltro;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Mapper.AbstractMapperComplete;
import com.blue.open.Core.Repository.EntityRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public abstract class AbstractService<T extends AbstractEntity, DTO extends AbstractEntityDTO, PAGINATEDTO extends AbstractEntityDTO> {

    @Autowired
    protected EntityRepository<T> repository;

    @Autowired
    protected AbstractMapperComplete<T, DTO, PAGINATEDTO> mapper;

    @Autowired
    public AuthService authService;

    private final Class<T> entityClass;

    // Hooks

    // Authenticaciones
    protected void hookUuidAuth(String uuid) {
    }

    protected void hookPaginateAuth(PaginateList paginateList) {
    }

    protected void hookSaveAuth(DTO model) {
    }

    protected void hookDeleteAuth(String uuid) {
    }

    // Recibe el uuid de un registro y lo busca en la base de datos
    protected T hookUuidFindDatabase(String uuid) {
        return this.repository.findFirstByUuid(uuid);
    }

    // Recibe el entity de un registro y devuelve el DTO del mismo
    protected DTO hookUuidMapperModelToDto(T model) {
        return this.mapper.modelToDto(model);
    }

    // Fuerza a responder registros con status 0
    protected PaginateList hookPaginateFilterStatus(PaginateList paginateList) {
        paginateList.addFiltro(List.of(new PaginateFiltro("status", "0")));
        return paginateList;
    }

    // Agrega parametros de busqueda y ordedamiento previos a ejecutar la consulta
    protected PaginateList hookPaginatePreQuery(PaginateList paginateList) {
        return paginateList;
    }

    // Ejecuta la query para paginar
    protected Page<T> hookPaginateQueryExecute(PaginateList paginateList) {
        return this.repository.paginate(paginateList);
    }

    // Mappea la respuesta de la query al DTO de paginado
    protected List<PAGINATEDTO> hookPaginateMapperModelToPaginateDto(Page<T> page) {
        return this.mapper.modelsToPaginateDtos(page.getContent());
    }

    // Permite filtrar los resultados del paginado despues de ser mapeados a DTO
    protected List<PAGINATEDTO> hookPaginateFilterResults(List<PAGINATEDTO> dtoList) {
        return dtoList;
    }

    // Crea una PageImpl desde un listado
    protected Page<PAGINATEDTO> hookPaginatePageImplCreate(Page<T> page, List<PAGINATEDTO> dtoList) {
        return new PageImpl<>(dtoList, page.getPageable(), page.getTotalElements());
    }

    // Si el DTO contiene un UUID, busca un registro en la db por UUID
    protected T hookSaveGetExistRegister(DTO dto) {
        if (dto.getUuid() != null && !dto.getUuid().isEmpty()) {
            return this.repository.findFirstByUuidWithStatusIsActive(dto.getUuid());
        }
        return null;
    }

    // Si hookSaveGetExistRegister(dto) no encontro ningun registro, esta funcion
    // crea un registro vacio
    protected T hookSaveCreateNewEmptyModel(DTO dto) {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new CustomException("Error creando la entidad", e.getMessage());
        }
    }

    // Establece el usuario creador del registro
    protected T hookSaveSetUserCreated(T model) {
        User user = (User) this.authService.getActualOrNull();
        if (user != null)
            model.setIdUserCreated(user.getId());
        return model;
    }

    // Mappea el DTO a un modelo. Si modelo ya existe entonces remplaza los datos
    // modificando el registro original
    protected T hookSaveMapperDtoToModel(DTO dto, T model) {
        if (model == null) {
            throw new CustomException("Error el registro esta vacio",
                    "Error hookSaveMapperDtoToModel recibe un null de entrada");
        }
        model = this.mapper.dtoToModel(dto, model);
        if (model == null) {
            throw new CustomException("Error al convertir el modelo",
                    "Error hookSaveMapperDtoToModel dtoToModel responde null");
        }
        return model;
    }

    // Establece el último usuario que edito el registro
    protected T hookSaveSetUserUpdated(T model) {
        User user = (User) this.authService.getActualOrNull();
        if (user != null && model != null)
            model.setIdUserUpdated(user.getId());
        return model;
    }

    // Guarda el registro nuevo, o el editado, en la base de datos
    protected T hookSaveDatabaseSave(T model) {
        return this.repository.save(model);
    }

    // Mappea el modelo a DTO
    protected DTO hookSaveMapperModelToDto(T model) {
        return this.mapper.modelToDto(model);
    }

    // Busca el registro a borrar usando su uuid
    protected T hookDeleteFindByUuid(String uuid) {
        return this.repository.findFirstByUuidWithStatusIsActive(uuid);
    }

    // Setea el borrado logico en el registro
    protected T hookDeleteSetStatusDeleted(T model) {
        model.setStatus(-1);
        model.setDateDeleted(Timestamp.from(Instant.now()));
        return model;
    }

    // Establece el usuario que borro el registro
    protected T hookDeleteSetUserDeleted(T model) {
        User user = (User) this.authService.getActualOrNull();
        if (user != null)
            model.setIdUserDeleted(user.getId());
        return model;
    }

    // Guarda el registro borrado en la database
    protected T hookDeleteSaveDeleted(T model) {
        return this.repository.save(model);
    }

    // Mappea el entity a DTO
    protected DTO hookDeleteMapperModelToDto(T model) {
        return this.mapper.modelToDto(model);
    }

    @SuppressWarnings("unchecked")
    protected AbstractService() {
        Class<?> current = getClass();
        Class<T> targetEntityClass = null;

        while (current != null && current != Object.class) {
            Type genericSuperclass = current.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericSuperclass;
                if (pt.getRawType() == AbstractService.class) {
                    Type typeArg = pt.getActualTypeArguments()[0];
                    if (typeArg instanceof Class) {
                        targetEntityClass = (Class<T>) typeArg;
                        break;
                    }
                }
            }
            current = current.getSuperclass();
        }
        this.entityClass = targetEntityClass;
    }


    // getters para entityClass si lo necesitás
    protected Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * Busca por UUID
     */
    @Transactional(readOnly = true)
    public DTO getByUuid(String uuid) {
        try {
            this.hookUuidAuth(uuid);
            T model = this.hookUuidFindDatabase(uuid);
            if (model == null)
                throw new CustomException("No existe el registro");
            return hookUuidMapperModelToDto(model);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener", "Error en getByUuid: " + e.getMessage());
        }
    }

    /**
     * Paginación genérica
     */
    @Transactional(readOnly = true)
    public Page<PAGINATEDTO> paginate(PaginateList paginateList) throws Exception {
        try {
            this.hookPaginateAuth(paginateList);
            paginateList = this.hookPaginateFilterStatus(paginateList);
            paginateList.setEntityClass(this.getEntityClass());
            paginateList = this.hookPaginatePreQuery(paginateList);
            if (!paginateList.isInitialized()) {
                throw new CustomException("Error al inicilizar la paginacion",
                        "Error en el service del backend, falta hacer @Override de hookPaginatePreQuery() y hacer el paginateList.init(.., ..)");
            }
            Page<T> page = this.hookPaginateQueryExecute(paginateList);
            List<PAGINATEDTO> dtoList = this.hookPaginateMapperModelToPaginateDto(page);
            dtoList = this.hookPaginateFilterResults(dtoList);
            return this.hookPaginatePageImplCreate(page, dtoList);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener el paginado", "Error en paginate: " + e.getMessage());
        }
    }

    /**
     * Guardar o actualizar
     */
    @Transactional
    public DTO saveOrCreate(DTO dto) {
        try {
            this.hookSaveAuth(dto);
            T model = this.hookSaveGetExistRegister(dto);
            if (model == null) {
                model = this.hookSaveCreateNewEmptyModel(dto);
                model = this.hookSaveSetUserCreated(model);
            }
            model = this.hookSaveMapperDtoToModel(dto, model);
            model = this.hookSaveSetUserUpdated(model);
            model = this.hookSaveDatabaseSave(model);
            return hookSaveMapperModelToDto(model);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al guardar el registro", "Error en saveOrCreate: " + e.getMessage());
        }
    }

    /**
     * Eliminar (soft delete con status = -1)
     */
    @Transactional
    public DTO delete(String uuid) {
        try {
            this.hookDeleteAuth(uuid);
            T model = this.hookDeleteFindByUuid(uuid);
            if (model == null)
                throw new CustomException("No existe o ya fue eliminado");
            model = this.hookDeleteSetStatusDeleted(model);
            model = this.hookDeleteSetUserDeleted(model);
            model = this.hookDeleteSaveDeleted(model);
            return this.hookDeleteMapperModelToDto(model);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al borrar el registro", "Error en delete: " + e.getMessage());
        }
    }
}
