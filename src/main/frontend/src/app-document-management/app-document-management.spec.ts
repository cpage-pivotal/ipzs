import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppDocumentManagement } from './app-document-management';

describe('AppDocumentManagement', () => {
  let component: AppDocumentManagement;
  let fixture: ComponentFixture<AppDocumentManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppDocumentManagement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AppDocumentManagement);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
