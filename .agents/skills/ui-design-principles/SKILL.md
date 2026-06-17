---
name: ui-design-principles
description: Apply pragmatic UI design principles for fast, consistent, accessible interfaces across JavaFX, web, desktop, mobile, and CLI surfaces. Use this skill when designing or reviewing Bisq desktop UI, implementing JavaFX views, improving frontend features, checking layout quality, or evaluating user experience across React, SwiftUI, Flutter, or other UI frameworks.
---

# UI Design Principles

## Overview

Apply proven design principles from industry leaders (Apple, Vercel, shadcn/ui) to create interfaces that are fast, consistent, and delightful regardless of platform or framework. This skill provides actionable guidelines for speed optimization, spatial consistency, progressive disclosure, and immediate feedback that apply to Bisq JavaFX screens, web applications, desktop software, mobile apps, and command-line interfaces.

## Core Design Principles

### 1. Speed Through Subtraction (Apple)

**Philosophy**: "Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away."

**Universal Practices**:
- Remove unnecessary interactions - Make common actions 1-click/tap, rare actions 2-clicks/taps
- Reduce visual noise - Eliminate decorative elements that don't serve users
- Eliminate confirmation dialogs for low-risk actions - Only confirm destructive/irreversible operations
- Streamline workflows - Cut steps that don't add value

**Cross-Platform Examples**:
```
❌ Bad: Click "Edit" → Confirm "Are you sure?" → Click "Yes" → Edit mode
✅ Good: Click "Edit" → Edit mode (action is reversible)

❌ Bad: Settings screen with 50 visible options
✅ Good: Settings with categories, common settings visible, advanced collapsed

❌ Bad: CLI requiring 5 flags for common operation
✅ Good: CLI with smart defaults, verbose flags for rare cases
```

**Framework-Specific Applications**:

**Web (React/Vue/Angular)**:
- Single-click actions without unnecessary confirmation modals
- Inline editing instead of modal dialogs for reversible changes

**Desktop (JavaFX/Swing/Electron)**:
- Menu items for rare actions, toolbar buttons for common ones
- Keyboard shortcuts bypass multi-step dialogs

**Mobile (iOS/Android/Flutter)**:
- Swipe gestures for common actions (delete, archive)
- Bottom sheets instead of multiple navigation steps

**CLI/TUI**:
- Smart defaults reduce required flags
- Interactive prompts only for ambiguous cases

**Anti-patterns**:
- Multiple confirmations for reversible actions
- Extra steps "for safety" when undo/rollback exists
- Showing all features upfront instead of progressive disclosure

### 2. Spatial Consistency

**Philosophy**: "UI elements should have predictable positions and behaviors."

**Universal Practices**:
- Fixed positions for primary actions - CTAs don't move around
- Consistent spacing rhythm - Use systematic scale (4px base recommended)
- Stable layouts - Don't shift content during interactions
- Persistent navigation - Keep nav/search in same location

**Cross-Platform Examples**:
```
❌ Bad: Primary button sometimes top-right, sometimes bottom-left
✅ Good: Primary action always in same location (platform-appropriate)

❌ Bad: Layout shifts when loading content (CLS issue)
✅ Good: Reserve space for content with skeleton loaders/placeholders

❌ Bad: Spacing varies inconsistently throughout interface
✅ Good: All spacing follows rhythm (8, 16, 24, 32 units)
```

**Recommended Spacing Scale** (adapt to platform density):
```
Base unit: 4px (or 4dp/4pt depending on platform)

Micro:    4px  (tight spacing within components)
Small:    8px  (component internal padding)
Medium:   16px (spacing between related elements)
Large:    24px (spacing between component groups)
XLarge:   32px (section separation)
XXLarge:  48px (major layout divisions)
```

**Framework-Specific Applications**:

**Web (React/Vue/Angular)**:
```css
/* CSS Variables for consistency */
:root {
  --space-1: 4px;
  --space-2: 8px;
  --space-4: 16px;
  --space-6: 24px;
  --space-8: 32px;
}
```

**Desktop (JavaFX)**:
```java
// JavaFX Spacing with Insets
VBox layout = new VBox(16); // 16px spacing
layout.setPadding(new Insets(24, 24, 24, 24));
```

**Mobile (SwiftUI)**:
```swift
// SwiftUI Spacing
VStack(spacing: 16) {
    // Elements with consistent spacing
}
.padding(.horizontal, 24)
```

**CLI/TUI**:
- Consistent indentation levels (2 or 4 spaces)
- Predictable column alignment in tables
- Separator lines at consistent positions

**Anti-patterns**:
- Buttons that move based on content length
- Inconsistent padding across similar components
- Navigation that disappears or moves unexpectedly

### 3. Progressive Disclosure

**Philosophy**: "Show what matters now, reveal complexity when needed."

**Universal Practices**:
- Collapsed details by default - Show summary, hide details until needed
- Expand-in-place patterns - Avoid modals for viewing more information
- Contextual actions on interaction - Hide secondary actions until element is active
- Smart defaults - Pre-select common choices to reduce decisions

**Cross-Platform Examples**:
```
❌ Bad: FAQ page with all answers expanded (overwhelming)
✅ Good: FAQ with collapsed answers, expand individual items on demand

❌ Bad: Modal dialog to view profile details
✅ Good: Inline expansion showing additional details

❌ Bad: Edit/Delete buttons always visible on every list item
✅ Good: Actions appear on hover/focus/selection
```

**Framework-Specific Applications**:

**Web (React/Vue/Angular)**:
- Accordion components for collapsible sections
- Details/summary HTML elements
- Hover/focus states revealing contextual actions
- Dropdown menus for secondary options

**Desktop (JavaFX/Swing/Electron)**:
- Collapsible panels (TitledPane in JavaFX)
- Context menus on right-click
- Tree views with expandable nodes
- Tabbed interfaces for related content

**Mobile (iOS/Android/Flutter)**:
- Expandable list items (UITableView sections, RecyclerView with ViewHolders)
- Bottom sheets for additional options
- Swipe gestures to reveal actions
- Master-detail navigation patterns

**CLI/TUI**:
- Subcommands for advanced features
- `--help` flag for detailed options
- Interactive prompts for complex workflows
- Pagers for long output (less, more)

**Implementation Patterns by Platform**:

**Web Accordion**:
```tsx
// React accordion pattern
<Accordion>
  <AccordionItem>
    <AccordionTrigger>Summary (always visible)</AccordionTrigger>
    <AccordionContent>Details (revealed on click)</AccordionContent>
  </AccordionItem>
</Accordion>
```

**JavaFX Collapsible**:
```java
// JavaFX TitledPane (accordion)
TitledPane pane = new TitledPane("Summary", contentNode);
pane.setExpanded(false); // Collapsed by default
Accordion accordion = new Accordion(pane);
```

**SwiftUI Expandable**:
```swift
// SwiftUI DisclosureGroup
DisclosureGroup("Summary") {
    Text("Details revealed on tap")
}
```

**CLI Progressive Disclosure**:
```bash
# Basic command with smart defaults
$ git commit -m "message"

# Advanced options revealed with --help
$ git commit --help

# Interactive mode for complex workflows
$ git commit --interactive
```

**Anti-patterns**:
- Showing all options upfront (decision paralysis)
- Using modals when inline expansion would work
- Permanent visible actions for rarely-used features
- Deeply nested navigation requiring back-tracking

### 4. Feedback Immediacy

**Philosophy**: "Users should never wonder if their action worked."

**Universal Practices**:
- Instant visual feedback (<100ms) - Acknowledge user action immediately
- Optimistic updates - Update UI instantly, rollback if operation fails
- Subtle animations (200-300ms) - Smooth state transitions without delay
- Clear state transitions - Visually distinguish loading/success/error states

**Cross-Platform Examples**:
```
❌ Bad: Button shows no change when clicked, waits for response
✅ Good: Button changes to "Saving..." immediately, then "Saved ✓"

❌ Bad: Delete item, wait for spinner, item disappears
✅ Good: Item fades out immediately, rollback if operation fails

❌ Bad: Heavy 800ms animation that blocks interaction
✅ Good: 200-300ms subtle transition that feels smooth
```

**Universal State Indicators**:
- **Idle**: Default state, ready for interaction
- **Loading**: Visual spinner/progress indicator, <100ms to show
- **Success**: Checkmark/success indicator, 2-3 second auto-dismiss
- **Error**: Error indicator, error message, persistent until dismissed

**Animation Guidelines** (adapt to platform conventions):
- Micro-interactions: 100-200ms (button press, hover, selection)
- State transitions: 200-300ms (expand/collapse, fade)
- Scene transitions: 300-400ms (route change, modal, screen navigation)
- Never exceed 500ms for UI feedback

**Framework-Specific Applications**:

**Web (React/Vue/Angular)**:
```typescript
// Optimistic UI pattern
async function deleteItem(id: string) {
  // 1. Update UI immediately (optimistic)
  setItems(items.filter(item => item.id !== id));

  try {
    // 2. Call server
    await api.delete(`/items/${id}`);
    showToast('Item deleted', 'success');
  } catch (error) {
    // 3. Rollback on error
    setItems(originalItems);
    showToast('Failed to delete', 'error');
  }
}
```

**Desktop (JavaFX)**:
```java
// JavaFX button state feedback
button.setOnAction(event -> {
    // 1. Immediate visual feedback
    button.setText("Saving...");
    button.setDisable(true);

    // 2. Execute operation on background thread
    Task<Void> task = new Task<>() {
        @Override
        protected Void call() throws Exception {
            performSave();
            return null;
        }
    };

    // 3. Update UI on completion
    task.setOnSucceeded(e -> {
        button.setText("Saved ✓");
        Platform.runLater(() -> {
            Thread.sleep(2000);
            button.setText("Save");
            button.setDisable(false);
        });
    });

    task.setOnFailed(e -> {
        button.setText("Failed ✗");
        showErrorDialog(task.getException());
        button.setDisable(false);
    });

    new Thread(task).start();
});
```

**Mobile (SwiftUI)**:
```swift
// SwiftUI state-driven button
struct SaveButton: View {
    @State private var state: ButtonState = .idle

    var body: some View {
        Button(action: {
            state = .loading // Immediate feedback

            Task {
                do {
                    try await save()
                    state = .success
                    try await Task.sleep(nanoseconds: 2_000_000_000)
                    state = .idle
                } catch {
                    state = .error
                }
            }
        }) {
            Text(state.label)
        }
        .disabled(state == .loading)
    }
}

enum ButtonState {
    case idle, loading, success, error

    var label: String {
        switch self {
        case .idle: return "Save"
        case .loading: return "Saving..."
        case .success: return "✓ Saved"
        case .error: return "✗ Failed"
        }
    }
}
```

**CLI/TUI**:
```bash
# Immediate feedback patterns in CLI
$ git push
Counting objects: 100% (10/10), done.  # Progress feedback
Writing objects: 100% (10/10), 1.5 KiB | 1.5 MiB/s, done.
✓ Successfully pushed to origin/main  # Success indicator

# Error feedback
$ git push
✗ Error: failed to push some refs  # Clear error state
hint: Updates were rejected because...  # Actionable message
```

**Anti-patterns**:
- No visual response when user interacts
- Long operations without progress indication
- Heavy animations that feel sluggish (>500ms)
- Missing error states (silent failures)
- Blocking UI during background operations

## Design Review Checklist

Use this checklist when reviewing UI designs or implementations across any platform:

### Speed Audit
- [ ] Can any steps be removed from common workflows?
- [ ] Are there unnecessary confirmation dialogs for reversible actions?
- [ ] Is the most common action 1-click/tap away?
- [ ] Are rare/destructive actions appropriately protected (2-clicks)?
- [ ] Do keyboard shortcuts exist for power users (desktop)?
- [ ] Are smart defaults reducing user decisions?

### Consistency Audit
- [ ] Do primary actions stay in consistent positions?
- [ ] Is spacing using a systematic rhythm scale?
- [ ] Are layouts stable (no content layout shift)?
- [ ] Is navigation persistent and predictable?
- [ ] Do similar components use identical spacing/sizing?
- [ ] Are platform conventions followed (e.g., OK/Cancel order)?

### Disclosure Audit
- [ ] Are complex features hidden by default?
- [ ] Can users discover advanced features when needed?
- [ ] Are contextual actions revealed appropriately?
- [ ] Do smart defaults reduce decision-making?
- [ ] Is the information hierarchy clear (what's important)?
- [ ] Can users progressively learn advanced features?

### Feedback Audit
- [ ] Is visual feedback <100ms for all interactions?
- [ ] Are optimistic updates used for async operations?
- [ ] Are animations subtle and quick (200-300ms)?
- [ ] Are all states clearly distinguished (idle/loading/success/error)?
- [ ] Do long operations show progress indicators?
- [ ] Are errors actionable with clear next steps?

## Platform-Specific Considerations

### Web Applications

**Strengths**:
- Rich animations and transitions
- Hover states for progressive disclosure
- Optimistic updates with async/await patterns
- Browser back/forward navigation

**Challenges**:
- Network latency for remote operations
- Cross-browser compatibility
- Accessibility (keyboard navigation, screen readers)
- Performance on low-end devices

**Best Practices**:
- Use semantic HTML for accessibility
- Implement keyboard navigation (Tab, Enter, Escape)
- Provide loading states for network operations
- Test on mobile devices (touch targets)

### Desktop Applications (JavaFX, Swing, Electron)

**Strengths**:
- Rich keyboard shortcuts
- Context menus for advanced actions
- Multi-window/multi-pane layouts
- Direct file system access

**Challenges**:
- Platform-specific UI conventions (Windows vs macOS vs Linux)
- Window management complexity
- High user expectations for responsiveness
- Complex state management across windows

**Best Practices**:
- Follow platform conventions (menu order, button placement)
- Provide extensive keyboard shortcuts
- Use background threads (JavaFX Task, SwingWorker) for long operations
- Implement proper window lifecycle management

**JavaFX-Specific**:
- Use Platform.runLater() for UI thread updates
- Leverage FXML for declarative layouts
- Use CSS for consistent theming
- Implement Properties/Bindings for reactive UIs

### Mobile Applications (iOS, Android, Flutter)

**Strengths**:
- Touch gestures (swipe, pinch, long-press)
- Bottom sheets for contextual options
- Native animations and transitions
- Push notifications for feedback

**Challenges**:
- Limited screen space
- Touch target sizes (minimum 44pt/48dp)
- Battery/performance constraints
- Platform-specific design languages (Material/Human Interface)

**Best Practices**:
- Follow platform design guidelines (Material Design, Human Interface Guidelines)
- Use native navigation patterns (tab bar, nav bar, drawer)
- Implement pull-to-refresh for data updates
- Provide haptic feedback for actions
- Optimize for one-handed use

### Command-Line Interfaces (CLI/TUI)

**Strengths**:
- Fast for power users
- Scriptable and composable
- Low resource overhead
- Excellent for automation

**Challenges**:
- No visual feedback (text only)
- Discovery of features harder
- Error handling complexity
- Limited progressive disclosure

**Best Practices**:
- Provide comprehensive --help documentation
- Use colored output for state (green=success, red=error)
- Show progress for long operations (spinners, progress bars)
- Implement interactive modes for complex workflows
- Use pagers for long output
- Provide examples in help text

## When to Apply Each Principle

### Speed Through Subtraction
- **When**: Designing new features, refactoring complex flows, user feedback about tedious workflows
- **Signs needed**: Users complaining about "too many clicks", workflows feel tedious, high abandonment rates
- **Impact**: Reduced time-to-completion, lower cognitive load, improved conversion rates

### Spatial Consistency
- **When**: Building design systems, refactoring inconsistent UIs, scaling design to new features
- **Signs needed**: Users can't find features, layouts feel chaotic, design debt accumulating
- **Impact**: Improved learnability, reduced confusion, faster development (reusable patterns)

### Progressive Disclosure
- **When**: Building complex features, settings pages, advanced tools, professional software
- **Signs needed**: Users overwhelmed by options, interface feels cluttered, poor feature discovery
- **Impact**: Reduced cognitive load, improved discoverability, better user retention

### Feedback Immediacy
- **When**: Implementing async operations, form submissions, data mutations, long-running processes
- **Signs needed**: Users unsure if action worked, repeated clicks, support requests about "broken" features
- **Impact**: Increased confidence, reduced user anxiety, fewer support tickets

## Resources

This skill is guidance-only and does not include scripts, references, or assets. All implementation patterns are provided inline in the SKILL.md for immediate reference.

## Cross-Framework Pattern Reference

### State Management Patterns

**Web (React)**:
```typescript
const [state, setState] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
```

**JavaFX (Property)**:
```java
ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
state.addListener((obs, oldVal, newVal) -> updateUI(newVal));
```

**SwiftUI (State)**:
```swift
@State private var state: ButtonState = .idle
```

**Flutter (State)**:
```dart
enum ButtonState { idle, loading, success, error }
ButtonState _state = ButtonState.idle;
setState(() => _state = ButtonState.loading);
```

### Progressive Disclosure Patterns

**Web**: Accordion, Details/Summary, Collapsible sections
**JavaFX**: TitledPane, Accordion, TreeView
**SwiftUI**: DisclosureGroup, List with expandable sections
**Flutter**: ExpansionTile, ExpansionPanel
**CLI**: Subcommands, --help flags, interactive prompts

### Animation/Transition Patterns

**Web (CSS)**:
```css
transition: all 200ms ease-in-out;
```

**JavaFX (Timeline)**:
```java
Timeline timeline = new Timeline(
    new KeyFrame(Duration.millis(200),
        new KeyValue(node.opacityProperty(), 0))
);
timeline.play();
```

**SwiftUI (Animation)**:
```swift
withAnimation(.easeInOut(duration: 0.2)) {
    // State changes
}
```

**Flutter (Animation)**:
```dart
AnimationController(duration: Duration(milliseconds: 200), vsync: this);
```
