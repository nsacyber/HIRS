import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { RimDatabasePageService } from './rim-database-page-service';

describe('RimDatabasePageService', () => {
  let service: RimDatabasePageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(RimDatabasePageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
