import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ValidationReportsPageService } from './validation-reports-page-service';

describe('ValidationReportsPageService', () => {
  let service: ValidationReportsPageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(ValidationReportsPageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
