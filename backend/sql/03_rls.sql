ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY transactions_owner_policy ON transactions
  FOR ALL USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY categories_owner_policy ON categories
  FOR ALL USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);

ALTER TABLE budgets ENABLE ROW LEVEL SECURITY;
CREATE POLICY budgets_owner_policy ON budgets
  FOR ALL USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);

ALTER TABLE goals ENABLE ROW LEVEL SECURITY;
CREATE POLICY goals_owner_policy ON goals
  FOR ALL USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);

ALTER TABLE corrections ENABLE ROW LEVEL SECURITY;
CREATE POLICY corrections_owner_policy ON corrections
  FOR ALL USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);

ALTER TABLE ai_model_params ENABLE ROW LEVEL SECURITY;
CREATE POLICY ai_model_params_owner_policy ON ai_model_params
  FOR ALL USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);

ALTER TABLE anomaly_stats ENABLE ROW LEVEL SECURITY;
CREATE POLICY anomaly_stats_owner_policy ON anomaly_stats
  FOR ALL USING (user_id = auth.uid()::text) WITH CHECK (user_id = auth.uid()::text);