import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PolicyPageService } from './policy-page-service';

describe('PolicyPageService', () => {
  let service: PolicyPageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(PolicyPageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
