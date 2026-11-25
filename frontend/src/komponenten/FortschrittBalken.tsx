import { Box, LinearProgress, Typography } from '@mui/material'
import type { Fortschritt } from '../api/typen.ts'

// Fortschritt (US6): Balken plus Text. Der Wert kommt fertig berechnet vom Backend.
export default function FortschrittBalken({ fortschritt, text }: { fortschritt: Fortschritt; text: string }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
      <LinearProgress
        variant="determinate"
        value={fortschritt.prozent}
        color="success"
        aria-label={`Fortschritt ${fortschritt.prozent} %`}
        sx={{ flexGrow: 1, height: 10, borderRadius: 1, minWidth: 80 }}
      />
      <Typography variant="body2" sx={{ whiteSpace: 'nowrap' }}>
        {text}
      </Typography>
    </Box>
  )
}
