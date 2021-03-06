package com.rcsoyer.servicosjuridicos.service.impl;

import com.rcsoyer.servicosjuridicos.domain.CoordenacaoJuridica;
import com.rcsoyer.servicosjuridicos.repository.CoordenacaoJuridicaRepository;
import com.rcsoyer.servicosjuridicos.service.CoordenacaoJuridicaService;
import com.rcsoyer.servicosjuridicos.service.dto.CoordenacaoCreateUpdateDto;
import com.rcsoyer.servicosjuridicos.service.dto.QueryParamsCoordenacao;
import com.rcsoyer.servicosjuridicos.service.mapper.CoordenacaoJuridicaMapper;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service Implementation for managing CoordenacaoJuridica.
 */
@Slf4j
@Service
@Transactional
public class CoordenacaoJuridicaServiceImpl implements CoordenacaoJuridicaService {
    
    private final CoordenacaoJuridicaMapper mapper;
    private final CoordenacaoJuridicaRepository repository;
    
    public CoordenacaoJuridicaServiceImpl(final CoordenacaoJuridicaRepository coordenacaoJuridicaRepository,
                                          final CoordenacaoJuridicaMapper coordenacaoJuridicaMapper) {
        this.mapper = coordenacaoJuridicaMapper;
        this.repository = coordenacaoJuridicaRepository;
    }
    
    @Override
    public CoordenacaoCreateUpdateDto save(final CoordenacaoCreateUpdateDto dto) {
        log.debug("Transforming and  saving CoordenacaoJuridica: {}", dto);
        Function<CoordenacaoCreateUpdateDto, CoordenacaoJuridica> toEntity = mapper::toEntity;
        return toEntity.andThen(repository::save)
                       .andThen(mapper::toDto)
                       .apply(dto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<CoordenacaoCreateUpdateDto> findOne(Long id) {
        log.debug("Querying the DB to find a CoordenacaoJuridica with: id={}", id);
        return repository.findById(id)
                         .map(mapper::toDto);
    }
    
    @Override
    public void delete(Long id) {
        log.debug("Remove from DB CoordenacaoJuridica: {}", id);
        repository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CoordenacaoCreateUpdateDto> seekByParams(final QueryParamsCoordenacao queryParams,
                                                         final Pageable pageable) {
        return repository.query(mapper.toEntity(queryParams), pageable)
                         .map(mapper::toDto);
    }
}
