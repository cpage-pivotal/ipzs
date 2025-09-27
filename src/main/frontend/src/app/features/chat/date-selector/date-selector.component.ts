import { Component, input, output, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from '../../../shared/material/material.module';
import { TranslationService } from '../../../services/translation.service';

@Component({
  selector: 'app-date-selector',
  standalone: true,
  imports: [CommonModule, FormsModule, MaterialModule],
  template: `
    <div class="date-selector">
      <mat-form-field appearance="outline">
        <mat-label>{{ translationService.t('chat.dateLabel') }}</mat-label>
        <input
          matInput
          [matDatepicker]="picker"
          [(ngModel)]="selectedDate"
          (ngModelChange)="onDateChange($event)"
          readonly>
        <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
        <mat-datepicker #picker></mat-datepicker>
      </mat-form-field>
      <div class="date-info">
        <small>{{ translationService.t('chat.dateInfo') }}</small>
      </div>
    </div>
  `,
  styles: [`
    .date-selector {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .date-info {
      color: #666;
      font-size: 0.75rem;
    }

    mat-form-field {
      width: 200px;
    }
  `]
})
export class DateSelectorComponent {
  currentDate = input.required<Date>();
  dateChange = output<Date>();

  selectedDate = signal<Date>(new Date());

  constructor(public translationService: TranslationService) {
    effect(() => {
      this.selectedDate.set(this.currentDate());
    });
  }

  onDateChange(date: Date): void {
    if (date) {
      this.selectedDate.set(date);
      this.dateChange.emit(date);
    }
  }
}