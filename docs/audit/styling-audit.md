# UI Styling Audit

Read-only audit against `docs/shared/DESIGN_SYSTEM.md`.

## 1. Desktop (JavaFX)

### Inline FXML styles

- Inline `style="..."` usage is widespread, especially in:
  - `desktop/src/main/resources/fxml/teacher/assessments.fxml`
  - `desktop/src/main/resources/fxml/teacher/mark-entry.fxml`
  - `desktop/src/main/resources/fxml/main-layout.fxml`
  - `desktop/src/main/resources/fxml/teacher/class-roster.fxml`
  - `desktop/src/main/resources/fxml/teacher/my-classes.fxml`
  - `desktop/src/main/resources/fxml/report/reports.fxml`
  - `desktop/src/main/resources/fxml/login.fxml`
- Repeated inline styles cover page backgrounds, headings, subtitles, cards, buttons, navigation items, status labels, and metric labels. These should be shared CSS classes.
- Color casing and values vary (`#4f46e5`, `#4F46E5`, `white`, `#1e293b`), weakening palette consistency.

### Palette discrepancies

- Several FXML roots use `#f7f8fc` instead of the required background `#F8FAFC`.
- Primary text frequently uses `#1E293B` instead of `#0F172A`.
- `main-layout.fxml` uses `#eef4ff` instead of `#EEF2FF`.
- `teacher/mark-entry.fxml` uses `#16A34A` instead of success `#059669`.
- `teacher/assessments.fxml` uses `#FEF9C3/#854D0E` instead of warning `#FFFBEB/#B45309`.

### Typography

- Global CSS correctly declares `Inter`, with `Segoe UI` fallback, and uses `JetBrains Mono` for numeric/table styles.
- FXML headings often use `bold` or `800` instead of the specified heading weight 600.
- Several labels use 10px or 11px text; the design-system minimum is 12px.
- Search/profile/dialog controls use 6px, 7px, or 9px radii in places where inputs and buttons should use 8px.

### Tables and inputs

- `.modern-table` and `.assessment-table` largely match the system: slate headers, 48px rows, borders, hover/selected states, and mono numeric cells.
- Shared input styles largely match the required white background, slate borders, 40px height, 8px radius, and indigo focus.
- Inline FXML overrides create inconsistent table, input, border, focus, and typography behavior.
- Some dialog controls use 6px/7px radii instead of 8px.

## 2. Web (Thymeleaf)

### Existing alignment

- `layouts/base.html` and `layouts/auth.html` load Inter and JetBrains Mono with the correct fallbacks.
- Tailwind configuration maps the primary indigo palette correctly.
- Most cards use white surfaces, slate borders, `rounded-xl`, and `shadow-sm`.
- Grade badges follow the documented grade colors.

### Discrepancies

- `rounded-2xl` is used on cards and the results banner; cards should use `rounded-xl`. `rounded-2xl` is reserved for modals.
- `shadow-2xs` appears in inputs, badges, pagination, and action controls; the design system specifies `shadow-sm` as the component baseline.
- Inputs use `border-slate-300` and `focus:ring-1`; standardize on `border-slate-200` and a 2px primary focus ring.
- Focus colors vary between `indigo-600` and the reference `indigo-500`.
- Headings broadly use `font-bold`; headings should normally use weight 600.
- Labels and metadata frequently use 10px/11px/12px text, while form labels should be 14px/500 and general text should not fall below 12px.
- Italic feedback and guidance text conflicts with the rule to avoid italics for emphasis.
- `student/results.html` uses an inline `th:style` width for its progress bar.
- The results summary uses an unspecified dark indigo gradient rather than the restrained solid palette.
- Pagination uses `rounded-md`; controls should use `rounded-lg`.
- Tables should be checked for consistent slate-50 headers, 12px uppercase tracking, 48px rows, mono right-aligned numbers, hover/selected states, and visible keyboard focus.

## 3. PDF Generator

### Current fonts and colors

- `backend/src/main/java/com/musomi/manager/service/PdfService.java` uses built-in Helvetica for headings, body text, muted text, table cells, and grades.
- It does not use Inter or JetBrains Mono.
- Primary indigo `#4F46E5` and slate colors `#F8FAFC`, `#E2E8F0`, and `#64748B` are present.
- Grade text colors match the documented grade palette, but grade cells keep alternating row backgrounds instead of grade-specific light backgrounds.
- Body/title/table text uses pure black instead of `#0F172A` and secondary slate colors.

### Recommended polish

- Register/embed Inter-compatible fonts for UI text and JetBrains Mono for averages, counts, positions, and other numbers.
- Replace pure black with `#0F172A`, `#475569`, and `#64748B` according to hierarchy.
- Improve table headers with consistent indigo/slate treatment, 600-weight typography, and aligned numeric columns.
- Apply JetBrains Mono and right alignment to numeric columns.
- Standardize cell padding, row height, and `#E2E8F0` dividers; use `#F8FAFC` striping only where density warrants it.
- Apply grade-specific light backgrounds with matching text colors for A-F.
- Replace plain spacer paragraphs with deliberate report sections and spacing.
- Add an indigo/slate summary block for overall average, grade, and position to align the PDF with desktop and web.

## Priority

1. Centralize JavaFX styling and remove repeated FXML inline overrides.
2. Normalize web radii, shadows, focus states, and typography.
3. Upgrade PDF font embedding and numeric/grade table treatment.

