import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from evaluate_sound_feedback import summarize_texts


class SoundFeedbackTest(unittest.TestCase):
    def test_multiple_sounds_and_voluntary_missed_event(self):
        report = summarize_texts([
            "AURORA SENSE | Reconocimiento de sonidos\n"
            "[13:30:01] POSIBLE TIMBRE DE PUERTA · puntaje IA 92/100\n"
            "[13:30:02] VALIDACIÓN MANUAL · ACERTÓ · POSIBLE TIMBRE DE PUERTA · puntaje IA 92/100\n"
            "[13:30:03] POSIBLE PERRO LADRANDO · puntaje IA 60/100\n"
            "[13:30:04] VALIDACIÓN MANUAL · NO COINCIDE · POSIBLE PERRO LADRANDO · puntaje IA 60/100\n"
            "[13:30:05] OMISIÓN MANUAL · Música · informado por la persona que prueba\n"
        ])
        self.assertEqual(report["source_files"], 1)
        self.assertEqual(report["reviewed_detections"], 2)
        self.assertEqual(report["marked_correct"], 1)
        self.assertEqual(report["marked_incorrect"], 1)
        self.assertEqual(report["reported_omissions"], 1)
        self.assertEqual(report["per_label"]["POSIBLE PERRO LADRANDO"]["marked_incorrect"], 1)
        self.assertEqual(report["reported_omissions_by_type"]["Música"], 1)

    def test_non_reviewed_and_invalid_entries_not_claimed_as_accuracy(self):
        report = summarize_texts([
            "[13:01] POSIBLE TIMBRE DE PUERTA · puntaje IA 99/100\n"
            "[13:02] VALIDACIÓN MANUAL · ACERTÓ · POSIBLE TIMBRE DE PUERTA · puntaje IA 111/100\n",
            "[13:03] VALIDACIÓN MANUAL · ACERTÓ · POSIBLE MÚSICA · puntaje IA 80/100"
        ])
        self.assertEqual(report["source_files"], 2)
        self.assertEqual(report["reviewed_detections"], 1)
        self.assertNotIn("accuracy", report)
        self.assertIn("NOT an accuracy", report["warning"])


if __name__ == "__main__":
    unittest.main()
