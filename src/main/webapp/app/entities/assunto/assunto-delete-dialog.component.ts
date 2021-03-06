import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { Assunto } from '../../shared/model/assunto.model';
import { ASSUNTO_LIST_MODIFICATION } from './assunto.constants';
import { AssuntoService } from './assunto.service';

@Component({ selector: 'assunto-delete-dialog', templateUrl: './assunto-delete-dialog.component.html' })
export class AssuntoDeleteDialogComponent {
    assunto: Assunto;

    constructor(private assuntoService: AssuntoService, public activeModal: NgbActiveModal, private eventManager: JhiEventManager) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.assuntoService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: ASSUNTO_LIST_MODIFICATION,
                content: 'Deleted an assunto'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({ selector: 'assunto-delete-popup', template: '' })
export class AssuntoDeletePopupComponent implements OnInit, OnDestroy {
    private ngbModalRef: NgbModalRef;

    constructor(private activatedRoute: ActivatedRoute, private router: Router, private modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ assunto }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(AssuntoDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
                this.ngbModalRef.componentInstance.assunto = assunto;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                        this.ngbModalRef = null;
                    }
                );
            }, 0);
        });
    }

    ngOnDestroy() {
        this.ngbModalRef = null;
    }
}
