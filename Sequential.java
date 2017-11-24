package distributedHungarian;

public class Sequential {
	int[][] c;
	int[][] z;
	int n;
	int[] m;
	int min;
	int[] row;
	int[] col;
	int m_row, m_col, n_row, n_col, e_row, e_col;
	int[] sol;
	int[] a;

	public Sequential(int[][] c, int n) {
		this.c = c;
		this.n = n;
		m = new int[n];
		a = new int[n];
		z = new int[n][n];
		row = new int[n];
		col = new int[n];
		min = Integer.MAX_VALUE;
		m_row = Integer.MAX_VALUE;
		m_col = Integer.MAX_VALUE;
		n_row = 0;
		n_col = 0;
		e_row = -1;
		e_col = -1;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				z[i][j] = 0;
			}
			m[i] = Integer.MAX_VALUE;
			row[i] = 0;
			col[i] = 0;
		}
	}

	public void solve() {
		// lines 1-4
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (c[i][j] < m[i])
					m[i] = c[i][j];
			}
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				c[i][j] -= m[i];
			}
		}
		// lines 5-7
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				if (c[i][j] < m[j])
					m[j] = c[i][j];
			}
		}
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				c[i][j] -= m[j];
			}
		}
		boolean bool = true;
		while (bool) {
			matching();

			if (a_is_full())
				break;
			cover();

			min = Integer.MAX_VALUE;
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (c[i][j] < min && row[i] == 0 && col[j] == 0) {
						min = c[i][j];
					}
				}
			}
			for (int i = 0; i < n; i++) {
				if (a[i] == -1 && min == Integer.MAX_VALUE) {
					bool = false;
				}
			}
			// line 10
			if (min != Integer.MAX_VALUE) {
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						if (row[i] == 0)
							c[i][j] -= min;
						if (col[j] == 1)
							c[i][j] += min;
					}
				}
			}
		}
	}

	private void cover() {
		for (int i = 0; i < n; i++) {
			row[i] = 0;
			col[i] = 0;
		}
		// For each row with no circled zeroes, mark the row by star
		boolean has_circled_zero;
		for (int i = 0; i < n; i++) {
			has_circled_zero = false;
			for (int j = 0; j < n; j++) {
				if (c[i][j] == 0 && z[i][j] == 1)
					has_circled_zero = true;
			}
			if (!has_circled_zero)
				row[i] = 1;

		}
		while (!zeroes_covered()) {
			// Examine the marked rows. If it contains any 0 with *, mark column where 0
			// stays by star.
			for (int j = 0; j < n; j++) {
				for (int i = 0; i < n; i++) {
					if (c[i][j] == 0 && row[i] == 1 && z[i][j] == -1)
						col[j] = 1;
				}
			}

			// eXAMINE MARKED COLUMNS, if it contains nay circled 0, mark that row by star
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (c[i][j] == 0 && z[i][j] == 1 && col[j] == 1)
						row[i] = 1;
				}
			}
		}
		for (int i = 0; i < n; i++) {
			if (row[i] == 1)
				row[i] = 0;
			else if (row[i] == 0)
				row[i] = 1;
		}
	}

	public void matching() {
		for (int i = 0; i < n; i++) {
			a[i] = -1;
			for (int j = 0; j < n; j++)
				z[i][j] = 0;
		}
		boolean did_assign;
		while (!all_zeroes_marked()) {
			e_row = -1;
			e_col = -1;
			n_row = 0;
			n_col = 0;
			did_assign = false;
			for (int i = 0; i < n; i++) {
				n_row = 0;
				for (int j = 0; j < n; j++) {
					if (c[i][j] == 0 && z[i][j] == 0) {
						n_row++;
						e_col = j;
					}
				}
				if (n_row == 1) {
					did_assign = true;
					e_row = i;
					z[e_row][e_col] = 1;
					a[e_col] = e_row;
					for (int k = 0; k < n; k++) {
						if (c[k][e_col] == 0 && k != e_row)
							z[k][e_col] = -1;
						if (c[e_row][k] == 0 && k != e_col)
							z[e_row][k] = -1;
					}
					break;
				}
			}
			if (!did_assign) {
				for (int j = 0; j < n; j++) {
					n_col = 0;
					for (int i = 0; i < n; i++) {
						if (c[i][j] == 0 && z[i][j] == 0) {
							n_col++;
							e_row = i;
						}
					}
					if (n_col == 1) {
						did_assign = true;
						e_col = j;
						z[e_row][e_col] = 1;
						a[e_col] = e_row;
						for (int k = 0; k < n; k++) {
							if (c[k][e_col] == 0 && k != e_row)
								z[k][e_col] = -1;
							if (c[e_row][k] == 0 && k != e_col)
								z[e_row][k] = -1;
						}

						break;
					}
				}
			}
			if (!did_assign) {
				e_row = -1;
				e_col = -1;
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						if (c[i][j] == 0 && z[i][j] == 0) {
							e_row = i;
							e_col = j;
						}
					}

				}
				if (e_row != -1 && e_col != -1) {
					z[e_row][e_col] = 1;
					a[e_col] = e_row;
					for (int k = 0; k < n; k++) {
						if (c[k][e_col] == 0 && k != e_row)
							z[k][e_col] = -1;
						if (c[e_row][k] == 0 && k != e_col)
							z[e_row][k] = -1;
					}
				}
			}
		}

	}

	private boolean row_has_one_unmarked_zero(int i) {
		int numzeros = 0;
		for (int j = 0; j < n; j++) {
			if (c[i][j] == 0 && z[i][j] == 0)
				numzeros++;
		}
		if (numzeros == 1)
			return true;
		else
			return false;
	}

	private boolean has_negative_one(int[] a) {
		for (int i = 0; i < n; i++) {
			if (a[i] == -1)
				return true;
		}
		return false;
	}

	private boolean zkj_is_zero(int j) {
		for (int k = 0; k < n; k++) {
			if (z[k][j] != 0)
				return false;
		}
		return true;
	}

	private boolean zeroes_covered() {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (c[i][j] == 0 && row[i] == 1 && col[j] == 0)
					return false;
			}
		}
		return true;
	}

	private boolean a_is_full() {
		for (int i = 0; i < n; i++) {
			if (a[i] == -1)
				return false;
		}
		return true;
	}

	private boolean all_zeroes_marked() {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (c[i][j] == 0 && z[i][j] == 0)
					return false;
			}
		}
		return true;
	}
}
