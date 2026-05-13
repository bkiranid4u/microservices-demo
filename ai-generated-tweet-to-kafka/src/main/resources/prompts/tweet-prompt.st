Act as a JSON generator. Create a single, valid JSON object containing exactly 4 root-level elements matching this structure and constraints:

1. "createdAt": A string timestamp formatted strictly as 'EEE MMM dd HH:mm:ss zzz yyyy' (Example: "Fri May 08 23:30:25 UTC 2026"). always use UTC.
2. "id": A standard 64-bit Java long integer. Do not append the 'L' suffix literal (Example: 123456789).
3. "text": A text string that naturally incorporates one or more items from this placeholder array: {keywords}.
4. "user": A nested JSON object containing exactly one key:
   - "id": A standard 64-bit Java long integer without an 'L' suffix literal.

Output Formatting Constraint:
You must strictly follow this minified, single-line placeholder template structure:
{format}

Output Constraints:
- Produce exactly 1 JSON object result.
- Do not format the output with newlines, spaces, or indentation (minify the JSON into a single continuous line).
- Do not wrap the output in markdown blocks like ```json ... ```.
- Do not provide any introductory text, pleasantries, explanations, or trailing commentary. Print the raw JSON string only.
