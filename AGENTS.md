# LocalVibe / Moon Player Project Rules

1. Do not move into music or novel modules before the video module is completed.
2. Do not fake success. A feature that is not truly working must not be reported as completed.
3. High-risk work must not be skipped with a simple "defer it". It must be split, researched, and validated in small steps.
4. When UI feedback repeats, do not keep coding blindly. Confirm the intended visual design and interaction first.
5. Before building a new dialog, panel, or complex UI, remind the user to create a mockup with Stitch or Product Design first.
6. Implement new UI only after the user confirms the mockup or screenshot.
7. Modify only the files explicitly allowed by the current task.
8. Do not auto-commit unless the user explicitly asks.
9. Do not auto-push.
10. Real-device behavior requires user confirmation, including player UI, dialogs, panels, gestures, subtitles, sleep timer, AB repeat, equalizer audio, deletion, and removal.
11. ADB must use E:\Android\platform-tools\adb.exe -P 62001.
12. Treat only crashes with `Process: com.shenghui.localvibe` as app crashes.
13. Do not misread system, MIUI, or other-process logs as app crashes.
14. Do not add dependencies, permissions, or Gradle changes unless the task explicitly allows them.
15. Every task result must include start status, changes made, unchanged confirmation, test results, and user confirmation items.
