import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CoordenacaoJuridica} from '../../shared/model/coordenacao-juridica.model';
import {CoordenacaoJuridicaService} from './coordenacao-juridica.service';
import {UpdateComponentAbastract} from '../../shared/components-abstract/update.component.abstract';
import {AssuntoService} from 'app/entities/assunto';
import {IMultiSelectSettings, IMultiSelectTexts} from 'angular-2-dropdown-multiselect';
import {MultiSelectSettings} from 'app/shared/util/multiselect/multiselect.settings';
import {JhiAlertService} from 'ng-jhipster';
import {AssuntosOptions} from './assuntos-options';
import * as _ from 'lodash';
import {MaskNumberUtils} from '../../shared/util/masknumber-utils/masknumber-utils';
import * as R from 'ramda';

@Component({
    selector: 'coordenacao-juridica-update',
    templateUrl: './coordenacao-juridica-update.component.html'
})
export class CoordenacaoJuridicaUpdateComponent extends UpdateComponentAbastract<CoordenacaoJuridica> implements OnInit {

    assuntosModel: number[];
    optionsTexts: IMultiSelectTexts;
    optionsSettings: IMultiSelectSettings;

    constructor(coordenacaoJuridicaService: CoordenacaoJuridicaService,
                activatedRoute: ActivatedRoute, private assuntoService: AssuntoService,
                private multiSelectSettings: MultiSelectSettings,
                private jhiAlertService: JhiAlertService, private assuntosOptions: AssuntosOptions,
                private maskNumberUtils: MaskNumberUtils) {
        super(coordenacaoJuridicaService, activatedRoute);
    }

    ngOnInit() {
        this.onInit();
        this.setOptionsSettings();
        this.defineTituloPagina('Coordenação Jurídica');
        this.assuntosOptions.queryAllAssuntos();
    }

    private setOptionsSettings() {
        this.optionsTexts = this.multiSelectSettings.getTexts();
        this.optionsSettings = this.multiSelectSettings.getSettings();
    }

    getAssuntoSelectOptions() {
        return this.assuntosOptions.assuntoSelectOptions;
    }

    maskNumero(): Function {
        return this.maskNumberUtils.maskNumero(3);
    }

    save() {
        this.setAssuntosFromModel();
        super.save();
    }

    private setAssuntosFromModel() {
        const createAssuntosArray = () => this.model.assuntos = [];
        R.when(_.isEmpty, createAssuntosArray)(this.model.assuntos);
        this.assuntosModel.forEach(id => this.model.assuntos.push({id}));
    }
}
