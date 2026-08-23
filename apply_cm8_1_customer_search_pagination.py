#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path.cwd()
BRANCH = "feat/sixpay-customer-management-baseline"

def run(*args):
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True)

def guard():
    r = run("git", "rev-parse", "--show-toplevel")
    if r.returncode != 0 or Path(r.stdout.strip()).resolve() != ROOT.resolve():
        raise SystemExit("Run from the sixpay-connect repository root.")
    branch = run("git", "branch", "--show-current").stdout.strip()
    if branch != BRANCH:
        raise SystemExit(f"Expected branch {BRANCH}, got {branch}")

def create(rel, text):
    p = ROOT / rel
    if p.exists():
        if p.read_text(encoding="utf-8") == text:
            print(f"[skip] {rel}")
            return
        raise SystemExit(f"[stop] {rel} already exists with different content")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[create] {rel}")

def overwrite(rel, text):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"[stop] missing {rel}")
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[write] {rel}")

def replace_once(rel, old, new, label):
    p = ROOT / rel
    text = p.read_text(encoding="utf-8")
    if new in text:
        print(f"[skip] {label}")
        return
    if old not in text:
        raise SystemExit(f"[stop] pattern not found for {label} in {rel}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
    print(f"[update] {label}")

guard()

B = "backend/customer/src/main/java/com/sixpay/customer/management"
DREPO = B + "/domain/repository"
INPUT = B + "/application/port/input"
SERVICE = B + "/application/service"
PERSIST = B + "/infrastructure/persistence"
API = B + "/api"
RESP = API + "/response"
MIG = "backend/customer/src/main/resources/db/migration"

create(DREPO + "/CustomerSearchCriteria.java", """package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.CustomerStatus;

public record CustomerSearchCriteria(
        String niu,
        String legalName,
        CustomerStatus status,
        String financialInstitutionCode,
        int page,
        int size
) {
    public CustomerSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 0"
            );
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }
        niu = normalize(niu);
        legalName = normalize(legalName);
        financialInstitutionCode =
                normalize(financialInstitutionCode);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
""")

create(DREPO + "/CustomerSearchPage.java", """package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.Customer;

import java.util.List;

public record CustomerSearchPage(
        List<Customer> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
    public CustomerSearchPage {
        content = List.copyOf(content);
    }
}
""")

overwrite(DREPO + "/CustomerRepository.java", """package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;

import java.util.Optional;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId customerId);

    CustomerSearchPage search(CustomerSearchCriteria criteria);

    boolean existsById(CustomerId customerId);

    boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(
            String financialInstitutionCode,
            String bankingCustomerReference
    );
}
""")

overwrite(PERSIST + "/CustomerSpringDataRepository.java", """package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerSpringDataRepository
        extends JpaRepository<CustomerJpaEntity, UUID> {

    @EntityGraph(attributePaths = "bankAccounts")
    @Query(
            "select distinct customer "
                    + "from CustomerJpaEntity customer "
                    + "where customer.id = :customerId"
    )
    Optional<CustomerJpaEntity> findAggregateById(
            @Param("customerId") UUID customerId
    );

    @Query(
            "select customer "
                    + "from CustomerJpaEntity customer "
                    + "where (:niu is null "
                    + "or lower(customer.niu) "
                    + "like lower(concat('%', :niu, '%'))) "
                    + "and (:legalName is null "
                    + "or lower(customer.legalName) "
                    + "like lower(concat('%', :legalName, '%'))) "
                    + "and (:status is null "
                    + "or customer.status = :status) "
                    + "and (:financialInstitutionCode is null "
                    + "or lower(customer.financialInstitutionCode) "
                    + "= lower(:financialInstitutionCode))"
    )
    Page<CustomerJpaEntity> search(
            @Param("niu") String niu,
            @Param("legalName") String legalName,
            @Param("status") CustomerStatus status,
            @Param("financialInstitutionCode")
            String financialInstitutionCode,
            Pageable pageable
    );

    boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(
            String financialInstitutionCode,
            String bankingCustomerReference
    );
}
""")

# Convert repository adapter from findAll to paged search.
replace_once(
    PERSIST + "/CustomerRepositoryAdapter.java",
    "import java.util.List;\n",
    "",
    "remove List import"
)
if "import org.springframework.data.domain.PageRequest;" not in (ROOT / (PERSIST + "/CustomerRepositoryAdapter.java")).read_text(encoding="utf-8"):
    replace_once(
        PERSIST + "/CustomerRepositoryAdapter.java",
        "import org.springframework.stereotype.Repository;\n",
        "import org.springframework.data.domain.PageRequest;\n"
        "import org.springframework.data.domain.Sort;\n"
        "import org.springframework.stereotype.Repository;\n",
        "add paging imports"
    )
if "CustomerSearchCriteria" not in (ROOT / (PERSIST + "/CustomerRepositoryAdapter.java")).read_text(encoding="utf-8"):
    replace_once(
        PERSIST + "/CustomerRepositoryAdapter.java",
        "import com.sixpay.customer.management.domain.repository.CustomerRepository;\n",
        "import com.sixpay.customer.management.domain.repository.CustomerRepository;\n"
        "import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;\n"
        "import com.sixpay.customer.management.domain.repository.CustomerSearchPage;\n",
        "add search imports"
    )

replace_once(
    PERSIST + "/CustomerRepositoryAdapter.java",
    """    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

""",
    """    @Override
    @Transactional(readOnly = true)
    public CustomerSearchPage search(
            CustomerSearchCriteria criteria
    ) {
        var pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        var result = repository.search(
                criteria.niu(),
                criteria.legalName(),
                criteria.status(),
                criteria.financialInstitutionCode(),
                pageable
        );

        var content = result.getContent()
                .stream()
                .map(this::toDomain)
                .toList();

        return new CustomerSearchPage(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                result.isFirst(),
                result.isLast()
        );
    }

""",
    "repository paged search"
)

overwrite(INPUT + "/CustomerQueryUseCase.java", """package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;
import com.sixpay.customer.management.domain.repository.CustomerSearchPage;

public interface CustomerQueryUseCase {

    Customer findById(CustomerId customerId);

    CustomerSearchPage search(CustomerSearchCriteria criteria);
}
""")

service_file = SERVICE + "/CustomerManagementService.java"
service = (ROOT / service_file).read_text(encoding="utf-8")
service = service.replace("import java.util.List;\n", "")
if "CustomerSearchCriteria" not in service:
    service = service.replace(
        "import com.sixpay.customer.management.domain.repository.CustomerRepository;\n",
        "import com.sixpay.customer.management.domain.repository.CustomerRepository;\n"
        "import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;\n"
        "import com.sixpay.customer.management.domain.repository.CustomerSearchPage;\n"
    )
service = service.replace(
    """    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAll();
    }

""",
    """    @Override
    @Transactional(readOnly = true)
    public CustomerSearchPage search(
            CustomerSearchCriteria criteria
    ) {
        return repository.search(criteria);
    }

"""
)
(ROOT / service_file).write_text(service, encoding="utf-8", newline="\n")
print("[update] CustomerManagementService search")

create(RESP + "/CustomerPageResponse.java", """package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.repository.CustomerSearchPage;

import java.util.List;

public record CustomerPageResponse(
        List<CustomerResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
    public static CustomerPageResponse from(
            CustomerSearchPage page
    ) {
        return new CustomerPageResponse(
                page.content()
                        .stream()
                        .map(CustomerResponse::from)
                        .toList(),
                page.totalElements(),
                page.totalPages(),
                page.page(),
                page.size(),
                page.first(),
                page.last()
        );
    }
}
""")

controller_file = API + "/CustomerController.java"
controller = (ROOT / controller_file).read_text(encoding="utf-8")
if "CustomerPageResponse" not in controller:
    controller = controller.replace(
        "import com.sixpay.customer.management.api.response.CustomerResponse;\n",
        "import com.sixpay.customer.management.api.response.CustomerResponse;\n"
        "import com.sixpay.customer.management.api.response.CustomerPageResponse;\n"
    )
if "CustomerSearchCriteria" not in controller:
    controller = controller.replace(
        "import com.sixpay.customer.management.domain.model.CustomerId;\n",
        "import com.sixpay.customer.management.domain.model.CustomerId;\n"
        "import com.sixpay.customer.management.domain.model.CustomerStatus;\n"
        "import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;\n"
    )
if "jakarta.validation.constraints.Max" not in controller:
    controller = controller.replace(
        "import jakarta.validation.constraints.Size;\n",
        "import jakarta.validation.constraints.Max;\n"
        "import jakarta.validation.constraints.Min;\n"
        "import jakarta.validation.constraints.Size;\n"
    )
old = """    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_customer.read')")
    @Operation(summary = "List enrolled SIXPAY customers")
    public java.util.List<CustomerResponse> list() {
        return query.findAll()
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }
"""
new = """    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_customer.read')")
    @Operation(
            summary = "Search enrolled SIXPAY customers",
            description = "Optional filters are combined with AND semantics."
    )
    public CustomerPageResponse search(
            @RequestParam(required = false)
            @Size(max = 100) String niu,
            @RequestParam(required = false)
            @Size(max = 200) String legalName,
            @RequestParam(required = false)
            CustomerStatus status,
            @RequestParam(required = false)
            @Size(max = 32) String financialInstitutionCode,
            @RequestParam(defaultValue = "0")
            @Min(0) int page,
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size
    ) {
        return CustomerPageResponse.from(
                query.search(
                        new CustomerSearchCriteria(
                                niu,
                                legalName,
                                status,
                                financialInstitutionCode,
                                page,
                                size
                        )
                )
        );
    }
"""
if old not in controller:
    raise SystemExit("[stop] CustomerController list() baseline not found")
(ROOT / controller_file).write_text(
    controller.replace(old, new, 1),
    encoding="utf-8",
    newline="\n"
)
print("[update] CustomerController paged search")

create(MIG + "/V20260822.05__index_customer_management_search.sql", """CREATE INDEX ix_customer_management_customer_niu
    ON customer_management_customer (niu);

CREATE INDEX ix_customer_management_customer_status
    ON customer_management_customer (status);

CREATE INDEX ix_customer_management_customer_financial_institution
    ON customer_management_customer (financial_institution_code);

CREATE INDEX ix_customer_management_customer_created_at
    ON customer_management_customer (created_at DESC);
""")

# Frontend
F = "frontend/src/app/features/customers"
MODELS = F + "/models"
FAPI = F + "/api"
SERVICES = F + "/services"
COMP = F + "/components"

p = ROOT / (MODELS + "/customer-management.ts")
text = p.read_text(encoding="utf-8")
if "export interface CustomerSearchCriteria" not in text:
    text += """

export interface CustomerSearchCriteria {
  niu?: string;
  legalName?: string;
  status?: CustomerStatus;
  financialInstitutionCode?: string;
  page: number;
  size: number;
}

export interface CustomerPage {
  content: CustomerMaster[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}
"""
p.write_text(text, encoding="utf-8", newline="\n")
print("[update] customer-management.ts")

p = ROOT / (MODELS + "/customer-management.response.ts")
text = p.read_text(encoding="utf-8")
if "export interface CustomerPageResponse" not in text:
    text += """

export interface CustomerPageResponse {
  content: CustomerMasterResponse[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}
"""
p.write_text(text, encoding="utf-8", newline="\n")
print("[update] customer-management.response.ts")

p = ROOT / (FAPI + "/customer-management-api.mapper.ts")
text = p.read_text(encoding="utf-8")
if "CustomerPage," not in text:
    text = text.replace("  CustomerMaster,\n", "  CustomerMaster,\n  CustomerPage,\n")
if "CustomerPageResponse," not in text:
    text = text.replace("  CustomerMasterResponse,\n", "  CustomerMasterResponse,\n  CustomerPageResponse,\n")
if "export function mapCustomerPage" not in text:
    text += """

export function mapCustomerPage(
  response: CustomerPageResponse,
): CustomerPage {
  return {
    ...response,
    content: response.content.map(mapCustomerMaster),
  };
}
"""
p.write_text(text, encoding="utf-8", newline="\n")
print("[update] customer-management-api.mapper.ts")

p = ROOT / (FAPI + "/customer-management-api.client.ts")
text = p.read_text(encoding="utf-8")
if "CustomerSearchCriteria" not in text:
    text = text.replace(
        "} from '../models/customer-management.requests';\n",
        "} from '../models/customer-management.requests';\n"
        "import { CustomerSearchCriteria } from '../models/customer-management';\n"
    )
if "CustomerPageResponse," not in text:
    text = text.replace("  CustomerMasterResponse,\n", "  CustomerMasterResponse,\n  CustomerPageResponse,\n")
text = text.replace(
    """  list(): Observable<CustomerMasterResponse[]> {
    return this.http.get<CustomerMasterResponse[]>(CUSTOMERS);
  }
""",
    """  search(
    criteria: CustomerSearchCriteria,
  ): Observable<CustomerPageResponse> {
    let params = new HttpParams()
      .set('page', criteria.page)
      .set('size', criteria.size);

    if (criteria.niu) {
      params = params.set('niu', criteria.niu);
    }
    if (criteria.legalName) {
      params = params.set('legalName', criteria.legalName);
    }
    if (criteria.status) {
      params = params.set('status', criteria.status);
    }
    if (criteria.financialInstitutionCode) {
      params = params.set(
        'financialInstitutionCode',
        criteria.financialInstitutionCode,
      );
    }

    return this.http.get<CustomerPageResponse>(CUSTOMERS, {
      params,
    });
  }
"""
)
p.write_text(text, encoding="utf-8", newline="\n")
print("[update] customer-management-api.client.ts")

p = ROOT / (SERVICES + "/customer-management.service.ts")
text = p.read_text(encoding="utf-8")
if "mapCustomerPage," not in text:
    text = text.replace("  mapCustomerMaster,\n", "  mapCustomerMaster,\n  mapCustomerPage,\n")
if "CustomerPage," not in text:
    text = text.replace("  CustomerMaster,\n", "  CustomerMaster,\n  CustomerPage,\n  CustomerSearchCriteria,\n")
text = text.replace(
    """  list(): Observable<CustomerMaster[]> {
    return this.api.list().pipe(map((items) => items.map(mapCustomerMaster)));
  }
""",
    """  search(
    criteria: CustomerSearchCriteria,
  ): Observable<CustomerPage> {
    return this.api.search(criteria).pipe(map(mapCustomerPage));
  }
"""
)
p.write_text(text, encoding="utf-8", newline="\n")
print("[update] customer-management.service.ts")

overwrite(COMP + "/customer-master-list-page.component.ts", """import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  CustomerMaster,
  CustomerPage,
  CustomerSearchCriteria,
  CustomerStatus,
} from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-master-list-page',
  imports: [
    DatePipe,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    RouterLink,
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar title="Customers SIXPAY" />

    <div class="actions">
      @if (canCreate()) {
        <a routerLink="/customers/enroll">Enrôler un Customer</a>
      }
      <a routerLink="/customers/observed">Observed Customers</a>
    </div>

    <sp-card title="Recherche Customer">
      <form [formGroup]="searchForm" (ngSubmit)="search()">
        <mat-form-field>
          <mat-label>NIU</mat-label>
          <input matInput formControlName="niu" />
        </mat-form-field>

        <mat-form-field>
          <mat-label>Nom</mat-label>
          <input matInput formControlName="legalName" />
        </mat-form-field>

        <mat-form-field>
          <mat-label>Institution</mat-label>
          <input matInput formControlName="financialInstitutionCode" />
        </mat-form-field>

        <label>
          Statut
          <select formControlName="status">
            <option value="">Tous</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="SUSPENDED">SUSPENDED</option>
            <option value="CLOSED">CLOSED</option>
          </select>
        </label>

        <button type="submit">Rechercher</button>
        <button type="button" (click)="reset()">Réinitialiser</button>
      </form>
    </sp-card>

    <sp-card title="Customers enrôlés">
      @if (loading()) {
        <sp-loading label="Chargement des Customers" />
      } @else if (customers().length === 0) {
        <p>Aucun Customer ne correspond aux critères.</p>
      } @else {
        <div class="customer-grid">
          @for (customer of customers(); track customer.id) {
            <a class="customer-row" [routerLink]="['/customers', customer.id]">
              <strong>{{ customer.legalName }}</strong>
              <span>{{ customer.niu || 'NIU non renseigné' }}</span>
              <span>{{ customer.financialInstitutionCode }}</span>
              <span>{{ customer.status }}</span>
              <span>{{ customer.updatedAt | date: 'short' }}</span>
            </a>
          }
        </div>

        <div class="pagination">
          <button
            type="button"
            [disabled]="page()?.first || loading()"
            (click)="previousPage()"
          >
            Précédent
          </button>

          <span>
            Page {{ currentPage() + 1 }} / {{ displayTotalPages() }}
            — {{ page()?.totalElements ?? 0 }} résultat(s)
          </span>

          <button
            type="button"
            [disabled]="page()?.last || loading()"
            (click)="nextPage()"
          >
            Suivant
          </button>

          <label>
            Taille
            <select [value]="pageSize()" (change)="changePageSize($event)">
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>
        </div>
      }
    </sp-card>
  `,
  styles: [`
    .actions,
    .pagination {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      align-items: center;
      margin: 1rem 0;
    }

    form {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 1rem;
      align-items: center;
    }

    .customer-grid {
      display: grid;
      gap: .75rem;
    }

    .customer-row {
      display: grid;
      grid-template-columns: 2fr 1.2fr 1fr .8fr 1fr;
      gap: 1rem;
      padding: 1rem;
      border: 1px solid #ddd;
      border-radius: .5rem;
      text-decoration: none;
      color: inherit;
    }

    @media (max-width: 900px) {
      form,
      .customer-row {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class CustomerMasterListPageComponent {
  private readonly service = inject(CustomerManagementService);
  private readonly auth = inject(AuthenticationService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(true);
  protected readonly page = signal<CustomerPage | null>(null);
  protected readonly customers = signal<CustomerMaster[]>([]);
  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(20);

  protected readonly searchForm = this.fb.nonNullable.group({
    niu: [''],
    legalName: [''],
    status: [''],
    financialInstitutionCode: [''],
  });

  protected readonly canCreate = () =>
    this.auth.hasPermission('customer.create') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly displayTotalPages = () =>
    Math.max(this.page()?.totalPages ?? 0, 1);

  constructor() {
    this.load();
  }

  protected search(): void {
    this.currentPage.set(0);
    this.load();
  }

  protected reset(): void {
    this.searchForm.reset({
      niu: '',
      legalName: '',
      status: '',
      financialInstitutionCode: '',
    });
    this.currentPage.set(0);
    this.load();
  }

  protected previousPage(): void {
    if (this.currentPage() === 0) {
      return;
    }
    this.currentPage.update((value) => value - 1);
    this.load();
  }

  protected nextPage(): void {
    if (this.page()?.last) {
      return;
    }
    this.currentPage.update((value) => value + 1);
    this.load();
  }

  protected changePageSize(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.pageSize.set(Number(target.value));
    this.currentPage.set(0);
    this.load();
  }

  private load(): void {
    const value = this.searchForm.getRawValue();

    const niu = value.niu.trim();
    const legalName = value.legalName.trim();
    const financialInstitutionCode =
      value.financialInstitutionCode.trim();
    const status = value.status as CustomerStatus | '';

    const criteria: CustomerSearchCriteria = {
      page: this.currentPage(),
      size: this.pageSize(),
      ...(niu ? { niu } : {}),
      ...(legalName ? { legalName } : {}),
      ...(status ? { status } : {}),
      ...(financialInstitutionCode
        ? { financialInstitutionCode }
        : {}),
    };

    this.loading.set(true);

    this.service
      .search(criteria)
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((page) => {
        this.page.set(page);
        this.customers.set(page.content);
        this.currentPage.set(page.page);
      });
  }
}
""")

print()
print("CM-8.1 applied.")
print("Run:")
print("  ./mvnw -pl customer -am test")
print("  ./mvnw -pl customer -am verify -Pfull-tests")
print("  cd frontend && npm run lint && npm run test && npm run build:all")
print("  git diff --check")
print("  git status --short")
