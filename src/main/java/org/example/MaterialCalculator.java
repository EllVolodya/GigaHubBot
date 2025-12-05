package org.example;

/** Прості будівельні калькулятори для бота GigaHub.
 * Усі методи статичні – можна викликати без створення об'єкта:
 * double area = MaterialCalculator.calcWallArea(5, 2.7); */
public class MaterialCalculator {

    // ---------- 1. ФАРБА ----------

    /** Порахувати площу стін без вікон/дверей. */
    public static double calcWallArea(double perimeter, double height, double openings) {
        double area = perimeter * height - openings;
        return Math.max(area, 0); // щоб не було від’ємних значень
    }

    /** Порахувати скільки потрібно літрів фарби. */
    public static double calcPaintLiters(double area, double coveragePerLiter, int layers) {
        if (coveragePerLiter <= 0 || layers <= 0) return 0;
        return (area * layers) / coveragePerLiter;
    }

    /** Порахувати скільки банок фарби потрібно. */
    public static int calcPaintCans(double litersNeeded, double canVolume) {
        if (litersNeeded <= 0 || canVolume <= 0) return 0;
        return (int) Math.ceil(litersNeeded / canVolume);
    }

    // ---------- 2. ПЛИТКА (ПІДЛОГА / СТІНА) ----------

    /**
     * Кількість плитки з запасом.
     *
     * @param roomLength     довжина кімнати (м)
     * @param roomWidth      ширина кімнати (м)
     * @param tileLengthCm   довжина плитки (см)
     * @param tileWidthCm    ширина плитки (см)
     * @param reservePercent запас у % (наприклад 10)
     */
    public static int calcTiles(double roomLength, double roomWidth,
                                double tileLengthCm, double tileWidthCm,
                                double reservePercent) {

        double areaRoom = roomLength * roomWidth;                   // м²
        double tileArea = (tileLengthCm / 100.0) * (tileWidthCm / 100.0); // м²

        if (areaRoom <= 0 || tileArea <= 0) return 0;

        double tiles = areaRoom / tileArea;

        // додаємо запас
        double k = 1 + reservePercent / 100.0;
        tiles *= k;

        return (int) Math.ceil(tiles);
    }

    // ---------- 3. ЛАМІНАТ ----------

    /**
     * Кількість дощок ламінату.
     *
     * @param roomLength   довжина кімнати (м)
     * @param roomWidth    ширина кімнати (м)
     * @param boardLenCm   довжина дошки (см)
     * @param boardWidthCm ширина дошки (см)
     * @param reservePercent запас %
     */
    public static int calcLaminateBoards(double roomLength, double roomWidth,
                                         double boardLenCm, double boardWidthCm,
                                         double reservePercent) {

        double roomArea = roomLength * roomWidth;
        double boardArea = (boardLenCm / 100.0) * (boardWidthCm / 100.0);

        if (roomArea <= 0 || boardArea <= 0) return 0;

        double boards = roomArea / boardArea;
        boards *= (1 + reservePercent / 100.0);

        return (int) Math.ceil(boards);
    }

    /**
     * Кількість упаковок ламінату.
     *
     * @param boardsNeeded    потрібна кількість дощок
     * @param boardsPerPack   дощок в одній упаковці
     */
    public static int calcLaminatePacks(int boardsNeeded, int boardsPerPack) {
        if (boardsNeeded <= 0 || boardsPerPack <= 0) return 0;
        return (int) Math.ceil(boardsNeeded / (double) boardsPerPack);
    }

    // ---------- 4. ВАГОНКА / ОБШИВКА СТІН ----------

    /**
     * Кількість планок вагонки.
     *
     * @param wallLength      довжина стіни (м)
     * @param wallHeight      висота стіни (м)
     * @param boardWidthCm    ширина планки (см)
     * @param reservePercent  запас %
     */
    public static int calcCladdingBoards(double wallLength, double wallHeight,
                                         double boardWidthCm, double reservePercent) {

        double wallArea = wallLength * wallHeight;
        double boardArea = (boardWidthCm / 100.0) * wallHeight;

        if (wallArea <= 0 || boardArea <= 0) return 0;

        double boards = wallArea / boardArea;
        boards *= (1 + reservePercent / 100.0);

        return (int) Math.ceil(boards);
    }

    // ---------- 5. ПЛІНТУС ----------

    /**
     * Кількість планок плінтуса.
     *
     * @param perimeter      периметр кімнати (м)
     * @param barLength      довжина однієї планки (м), наприклад 2.5
     * @param reservePercent запас %
     */
    public static int calcBaseboards(double perimeter, double barLength, double reservePercent) {
        if (perimeter <= 0 || barLength <= 0) return 0;

        double lengthWithReserve = perimeter * (1 + reservePercent / 100.0);
        return (int) Math.ceil(lengthWithReserve / barLength);
    }

    // ---------- Допоміжне округлення ----------

    /** Округлити до 2 знаків після коми. */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
