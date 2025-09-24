import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PolicySettings } from './policy-settings';

describe('PolicySettings', () => {
  let component: PolicySettings;
  let fixture: ComponentFixture<PolicySettings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PolicySettings]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PolicySettings);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
