# NeoEssentials Cleanup Tasks

This document outlines cleanup tasks that should be completed in future updates to maintain code quality and remove deprecated components.

## Code Cleanup Tasks

### Remove Deprecated Classes
- [ ] Remove `StringToBooleanArgumentType.java` (replaced by `VanillaBooleanParser`)
- [ ] Remove `StringToBooleanArgumentInfo.java` (replaced by `VanillaBooleanParser`)
- [ ] Update import statements to ensure no references to these classes remain

### Remove Unused Code
- [ ] Scan for unused imports across the codebase
- [ ] Remove unused methods and variables
- [ ] Clean up commented-out code that is no longer needed

### Refactor and Consolidate
- [ ] Consider further consolidation of utility classes
- [ ] Evaluate if any additional command handling can be simplified
- [ ] Ensure consistent coding style across all files

## Documentation Cleanup

- [ ] Remove any remaining references to multi-version support
- [ ] Update all documentation to reflect server-side only implementation
- [ ] Create or update JavaDoc for all public classes and methods
- [ ] Ensure screenshots and examples in documentation are current

## Build System Improvements

- [ ] Clean up build scripts to remove multi-version support
- [ ] Optimize build process for single-target compilation
- [ ] Add automated testing for core components
- [ ] Consider adding static code analysis tools

## Future Enhancements

- [ ] Additional optimization for server performance
- [ ] Consider further modularization of features
- [ ] Evaluate if any client-specific features could be reimplemented in a server-only approach

## Testing Requirements

Before removing any code:
1. Execute the complete test plan in `SERVER_SIDE_TEST_PLAN.md`
2. Verify all server-side functionality works correctly
3. Ensure no regressions are introduced

## Implementation Notes

When performing cleanup:
- Make incremental changes with thorough testing between steps
- Keep logs of what was removed and why
- Document any areas that require special attention
- Update version numbers according to semantic versioning

## Timeline Recommendation

- Cleanup tasks should be performed after the current version has been thoroughly tested in production
- Consider creating a dedicated "cleanup" release that focuses solely on these tasks
- Allow at least one minor version cycle before removing deprecated components to give users time to adapt

Last Updated: July 9, 2025
